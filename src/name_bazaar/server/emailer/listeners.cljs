(ns name-bazaar.server.emailer.listeners
  (:require [cljs-web3.eth :as web3-eth]
            [cljs.core.async :refer [<! >! chan]]        
            [district0x.server.state :as state]
            [district0x.shared.utils :as d0x-shared-utils]
            [goog.format.EmailAddress :as email-address]
            [name-bazaar.server.contracts-api.district0x-emails :as district0x-emails-api]
            [name-bazaar.server.contracts-api.offering :as offering-api]
            [name-bazaar.server.contracts-api.offering-requests :as offering-requests-api]
            [name-bazaar.server.emailer.templates :as templates]
            [district0x.server.emailer.sendgrid :as sendgrid]                  
            [district0x.server.state :as state]
            [district0x.shared.encryption-utils :as encryption-utils]
            [taoensso.timbre :as logging])
   (:require-macros [cljs.core.async.macros :refer [go]]
                    [district0x.server.macros :refer [gotry]]))

(defonce event-listeners (atom []))

(defn validate-email [base64-encrypted-email]
  (when-not (empty? base64-encrypted-email)
    (let [email (encryption-utils/decode-decrypt (state/config :private-key)
                                                 base64-encrypted-email)]
      (when (email-address/isValidAddress email)
        email))))

(defn on-offering-added [server-state {:keys [:offering :node :owner :version] :as args}]
  (logging/info "Handling blockchain event" {:args args} ::on-offering-added)
  (gotry
    (let [[error offering-request] (<! (offering-requests-api/get-request @server-state {:offering-request/node node}))]
      (if error
        (logging/error "Error retrieving requests for offering" {:error error} ::on-offering-added)
        (let [{:keys [:offering-request/name 
                      :offering-request/requesters-count 
                      :offering-request/latest-round
                      :offering-request/node]} offering-request]
          (if (pos? requesters-count)
            (let [[_ addresses] (<! (offering-requests-api/get-requesters @server-state {:offering-request/node node
                                                                                         :offering-request/round (dec latest-round)}))]
              (for [address addresses]
                (let [[_ base64-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address address}))]
                  (when-let [to-email (validate-email base64-encrypted-email)]
                    (sendgrid/send-notification-email {:from-email "district0x@district0x.io"
                                                       :to-email to-email
                                                       :subject (str "Offering has been added: " name)
                                                       :content (templates/on-offering-added offering name)}
                                                      {:header (str name " offering added")
                                                       :button-title "See offering details"
                                                       :button-href (templates/form-link offering)}
                                                      #(logging/info "Success sending email to requesting address" {:address address} ::on-offering-added)
                                                      #(logging/error "Error sending email to requesting address" {:error %} ::on-offering-added))))))
            (logging/info "No requesters found for offering" {:offering offering} ::on-offering-added)))))))

(defn- on-auction-finalized
  [server-state offering original-owner winning-bidder name price]
  (logging/info "Auction has been finalized" {:offering offering} ::on-offering-bought)
  (gotry
    (let [[_ owner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address original-owner}))
          [_ winner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address winning-bidder}))]      
      (if-let [to-email (validate-email owner-encrypted-email)]
        (sendgrid/send-notification-email {:from-email "district0x@district0x.io"
                                           :to-email to-email
                                           :subject (str "Your auction was finalized: " name)
                                           :content (templates/on-auction-finalized :owner offering name price)}
                                          {:header (str name " auction has ended")
                                           :button-title "See auction details"
                                           :button-href (templates/form-link offering)}
                                          #(logging/info "Success sending email to owner" {:address original-owner} ::on-auction-finalized)
                                          #(logging/error "Error sending email to owner" {:error %} ::on-auction-finalized))
        (logging/warn "Empty or malformed email" {:address original-owner} ::on-auction-finalized))
      
      (if-let [to-email winner-encrypted-email]
        (sendgrid/send-notification-email {:from-email "district0x@district0x.io"
                                           :to-email to-email
                                           :subject (str "Auction was finalized: " name)
                                           :content (templates/on-auction-finalized :winner offering name price)}
                                          {:header (str name " auction has ended")
                                           :button-title "See auction details"
                                           :button-href (templates/form-link offering)}
                                          #(logging/info "Success sending email to winner" {:address winning-bidder})
                                          #(logging/error "Error sending email to winner" {:error %}))
        (logging/warn "Empty or malformed winner email" {:address winning-bidder} ::on-auction-finalized)))))

(defn- on-offering-bought [server-state offering original-owner name price]
  (logging/info "Offerings has been bought" {:offering offering} ::on-offering-bought)
  (gotry
   (let [[_ owner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address original-owner}))]     
     (when-let [to-email (validate-email owner-encrypted-email)]
       (sendgrid/send-notification-email {:from-email "district0x@district0x.io"
                                          :to-email to-email
                                          :subject (str "Your offering was bought: " name)
                                          :content (templates/on-offering-bought offering name price)}
                                         {:header (str name " was bought")
                                          :button-title "See offering details"
                                          :button-href (templates/form-link offering)}
                                         #(logging/info "Success sending email to owner" {:address original-owner} ::on-offering-bought)
                                         #(logging/error "Error sending email" {:error %} ::on-offering-bought))))))

(defn on-offering-changed [server-state {:keys [:offering :version :event-type :extra-data] :as args}]
  (logging/info "Handling blockchain event" {:args args} ::on-offering-changed)
  (gotry
    (let [[_ {:keys [:offering/name :offering/original-owner :offering/price :offering/end-time :offering/winning-bidder] :as result}] (<! (offering-api/get-offering @server-state offering))]
      (if winning-bidder
        (on-auction-finalized server-state offering original-owner winning-bidder name price)
        (on-offering-bought server-state offering original-owner name price)))))

(defn on-new-bid
  [server-state {:keys [:offering] :as args}]
  (logging/info "Handling blockchain event" {:args args} ::on-new-bid)
  (gotry
    (let [[_ {:keys [:offering/name :offering/original-owner :offering/price :offering/end-time :offering/winning-bidder] :as result}] (<! (offering-api/get-offering @server-state offering))
          [_ owner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address original-owner}))]
      (when-let [to-email (validate-email owner-encrypted-email)]
        (sendgrid/send-notification-email {:from-email "district0x@district0x.io"
                                           :to-email to-email
                                           :subject (str "Your auction received a new bid: " name)
                                           :content (templates/on-new-bid offering name price)}
                                          {:header (str name " auction")
                                           :button-title "See auction details"
                                           :button-href (templates/form-link offering)}
                                          #(logging/info "Success sending on-new-bid email" {:address original-owner} ::on-new-bid)
                                          #(logging/error "Error when sending on-new-bid email" {:error %} ::on-new-bid))))))

(defn stop-event-listeners! []
  (doseq [listener @event-listeners]
    (when listener
      (web3-eth/stop-watching! listener (fn [])))))

(defn setup-listener!
  ([server-state contract-key event-key callback]
   (setup-listener! server-state contract-key event-key false nil callback))
  ([server-state contract-key event-key retrieve-events? event-type callback]
   (web3-eth/contract-call (state/instance @server-state contract-key)
                           event-key
                           (if event-type
                             {:event-type event-type}
                             {})
                           (if retrieve-events?
                             {:from-block 0 :to-block "latest"}
                             "latest")
                           (fn [error {:keys [:args] :as response}]
                             (if error
                               (logging/error "Error when setting up a listener for event" {:error error :event-key event-key} ::setup-listener!)
                               (callback server-state args))))))

(defn setup-event-listeners! [server-state]
  (stop-event-listeners!)
  (reset! event-listeners
          [(setup-listener! server-state :offering-registry :on-offering-added on-offering-added)
           (setup-listener! server-state :offering-registry :on-offering-changed false "finalize" on-offering-changed)
           (setup-listener! server-state :offering-registry :on-offering-changed false "bid" on-new-bid)]))
