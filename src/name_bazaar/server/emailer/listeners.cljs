(ns name-bazaar.server.emailer.listeners
  (:require [cljs-web3.eth :as web3-eth]
            [cljs.core.async :refer [<! >! chan]]
            [district0x.server.state :as state]
            [goog.format.EmailAddress :as email-address]
            [name-bazaar.server.contracts-api.district0x-emails :as district0x-emails-api]
            [name-bazaar.server.contracts-api.offering :as offering-api]
            [name-bazaar.server.contracts-api.offering-requests :as offering-requests-api]
            [district0x.server.emailer.sendgrid :as sendgrid]
            [name-bazaar.server.emailer.templates :as templates]
            [district0x.server.state :as state]
            [district0x.shared.encryption-utils :as encryption-utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce event-listeners (atom []))

(defn validate-email [base64-encrypted-email]
  (when-not (empty? base64-encrypted-email)
    (let [email (encryption-utils/decode-decrypt (state/config :private-key)
                                                 base64-encrypted-email)]
      (when (email-address/isValidAddress email)
        email))))

(defn on-offering-added [server-state {:keys [:offering :node :owner :version] :as args}]
  (go
    (let [[error offering-request] (<! (offering-requests-api/get-request @server-state {:offering-request/node node}))]
      (if error
        (prn "An error has occured: " error)
        (let [{:keys [:offering-request/name 
                      :offering-request/requesters-count 
                      :offering-request/latest-round 
                      :offering-request/node]} offering-request]
          (when (pos? requesters-count)
            (let [[_ addresses] (<! (offering-requests-api/get-requesters @server-state {:offering-request/node node
                                                                                         :offering-request/round (dec latest-round)}))]
              (for [address addresses]
                (let [[_ base64-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address address}))]
                  (when-let [to-email (validate-email base64-encrypted-email)]
                    (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                                       :to-email to-email
                                                       :subject "Offering created"
                                                       :content (templates/on-offering-added offering name)}
                                                      #(prn "Success sending on-offering-added email")
                                                      #(prn "An error has occured: " %))))))))))))

(defn- on-auction-finalized
  [server-state offering original-owner winning-bidder name price]
  (go
    (let [[_ owner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address original-owner}))
          [_ winner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address winning-bidder}))]
      (when-let [to-email (validate-email owner-encrypted-email)]
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email to-email
                                           :subject "Auction was finalized"
                                           :content (templates/on-auction-finalized :owner offering name price)}
                                          #(prn "Success sending on-auction-finalized email")
                                          #(prn "An error has occured: " %)))
      (when-let [to-email winner-encrypted-email]
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email to-email
                                           :subject "Auction was finalized"
                                           :content (templates/on-auction-finalized :winner offering name price)}
                                          #(prn "Success sending email")
                                          #(prn "An error has occured: " %))))))

(defn- on-offering-bought [server-state offering original-owner name price]
  (go
    (let [[_ owner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address original-owner}))]
      (when-let [to-email (validate-email owner-encrypted-email)]
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email to-email
                                           :subject "Your offering was bought"
                                           :content (templates/on-offering-bought offering name price)}
                                          #(prn "Success sending on-offering-bought email")
                                          #(prn "An error has occured: " %))))))

(defn on-offering-changed [server-state {:keys [:offering :version :event-type :extra-data] :as args}]
  (go
    (let [[_ {:keys [:offering/name :offering/original-owner :offering/price :offering/end-time :offering/winning-bidder] :as result}] (<! (offering-api/get-offering @server-state offering))]
      (if winning-bidder
        (on-auction-finalized server-state offering original-owner winning-bidder name price)
        (on-offering-bought server-state offering original-owner name price)))))

(defn on-new-bid
  [server-state {:keys [:offering] :as args}]
  (go
    (let [[_ {:keys [:offering/name :offering/original-owner :offering/price :offering/end-time :offering/winning-bidder] :as result}] (<! (offering-api/get-offering @server-state offering))
          [_ owner-encrypted-email] (<! (district0x-emails-api/get-email @server-state {:district0x-emails/address original-owner}))]
      (when-let [to-email (validate-email owner-encrypted-email)]
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email to-email
                                           :subject "Your offering received a new bid"
                                           :content (templates/on-new-bid offering name price)}
                                          #(prn "Success sending on-new-bid email")
                                          #(prn "An error has occured: " %))))))

(defn stop-event-listeners! []
  (doseq [listener @event-listeners]
    (when listener
      (web3-eth/stop-watching! listener (fn [])))))

(defn setup-listener!
  ([server-state contract-key event-key callback]
   (setup-listener! server-state contract-key event-key true nil callback))
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
                               (prn "An error has occured: " error)
                               (callback server-state args))))))

(defn setup-event-listeners! [server-state]
  (stop-event-listeners!)
  (reset! event-listeners
          [(setup-listener! server-state :offering-registry :on-offering-added on-offering-added)
           (setup-listener! server-state :offering-registry :on-offering-changed false "finalize" on-offering-changed)
           (setup-listener! server-state :offering-registry :on-offering-changed false "bid" on-new-bid)]))

