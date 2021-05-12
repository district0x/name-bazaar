(ns name-bazaar.server.emailer
  (:require
    [cljs.core.async :refer [<!]]
    [district.encryption :as encryption]
    [district.sendgrid :refer [send-email]]
    [district.server.config :refer [config]]
    [district.server.web3 :refer [web3]]
    [district.server.web3-events :as web3-events]
    [district.shared.async-helpers :refer [safe-go]]
    [district0x.shared.utils :refer [hex-to-utf8]]
    [goog.format.EmailAddress :as email-address]
    [mount.core :as mount :refer [defstate]]
    [name-bazaar.server.contracts-api.district0x-emails :refer [get-email]]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-requests :refer [get-request get-requesters]]
    [name-bazaar.server.emailer.templates :as templates]
    [name-bazaar.server.generator]
    [taoensso.timbre :refer [info warn error]]))

(declare start)
(declare stop)
(defstate emailer
          :start (start (merge (:emailer @config)
                               (:emailer (mount/args))))
          :stop (stop emailer))

(def template-id "93c0f083-1fcc-4a47-ae7c-2c8aef50c3ea")
(def from "district0x@district0x.io")

(defn validate-email [base64-encrypted-email]
  (when-not (empty? base64-encrypted-email)
    (let [email (encryption/decode-decrypt (:private-key @emailer) base64-encrypted-email)]
      (when (email-address/isValidAddress email)
        email))))


(def info-text "Emailer handling blockchain event")
(def error-text "Error emailer handling blockchain event")


(defn on-offering-added [{:keys [:offering :node :owner :version] :as args}]
  (info info-text {:args args} ::on-offering-added)
  (safe-go
    (let [{:keys [:offering-request/name :offering-request/latest-round]} (<! (get-request {:offering-request/node node}))
          round (dec latest-round)
          requesters (<! (get-requesters {:offering-request/node node
                                          :offering-request/round round}))]
      (if (empty? requesters)
        (info "No requesters found for offering" {:offering offering :name name :node node :round round} ::on-offering-added)
        (doseq [requester requesters]
          (let [base64-encrypted-email (<! (get-email {:district0x-emails/address requester}))]
            (when-let [to (validate-email base64-encrypted-email)]
              (send-email {:from from
                           :to to
                           :subject (str "Offering has been added: " name)
                           :content (templates/on-offering-added offering name)
                           :on-success #(info "Success sending email to requesting address" {:address requester} ::on-offering-added)
                           :on-error #(error "Error sending email to requesting address" {:error %} ::on-offering-added)
                           :substitutions {:header (str name " offering added")
                                           :button-title "See offering details"
                                           :button-href (templates/form-link offering)}
                           :template-id template-id
                           :api-key (:api-key @emailer)
                           :print-mode? (:print-mode? @emailer)}))))))))


(defn- on-auction-finalized [offering original-owner winning-bidder name price]
  (info info-text {:offering offering} ::on-auction-finalized)
  (safe-go
    (let [owner-encrypted-email (<! (get-email {:district0x-emails/address original-owner}))
          winner-encrypted-email (<! (get-email {:district0x-emails/address winning-bidder}))
          offering-address (:offering/address offering)]
      (if-let [to (validate-email owner-encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "Your auction was finalized: " name)
                     :content (templates/on-auction-finalized :owner offering-address name price)
                     :substitutions {:header (str name " auction has ended")
                                     :button-title "See auction details"
                                     :button-href (templates/form-link offering-address)}
                     :on-success #(info "Success sending email to owner" {:address original-owner} ::on-auction-finalized)
                     :on-error #(error "Error sending email to owner" {:error %} ::on-auction-finalized)
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})
        (warn "Empty or malformed email" {:address original-owner} ::on-auction-finalized))

      (if-let [to winner-encrypted-email]
        (send-email {:from from
                     :to to
                     :subject (str "Auction was finalized: " name)
                     :content (templates/on-auction-finalized :winner offering-address name price)
                     :substitutions {:header (str name " auction has ended")
                                     :button-title "See auction details"
                                     :button-href (templates/form-link offering-address)}
                     :on-success #(info "Success sending email to winner" {:address winning-bidder})
                     :on-error #(error "Error sending email to winner" {:error %})
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})
        (warn "Empty or malformed winner email" {:address winning-bidder} ::on-auction-finalized)))))


(defn- on-offering-bought [offering original-owner name price]
  (info info-text {:offering offering} ::on-offering-bought)
  (safe-go
    (let [owner-encrypted-email (<! (get-email {:district0x-emails/address original-owner}))
          offering-address (:offering/address offering)]
      (when-let [to (validate-email owner-encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "Your offering was bought: " name)
                     :content (templates/on-offering-bought offering-address name price)
                     :on-success #(info "Success sending email to owner" {:address original-owner} ::on-offering-bought)
                     :on-error #(error "Error sending email to owner" {:error %} ::on-offering-bought)
                     :substitutions {:header (str name " was bought")
                                     :button-title "See offering details"
                                     :button-href (templates/form-link offering-address)}
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})))))


(defn on-new-bid [{:keys [:offering] :as args}]
  (info info-text {:args args} ::on-new-bid)
  (safe-go
    (let [{:keys [:offering/name
                  :offering/original-owner
                  :offering/price]} (<! (offering/get-offering offering))
          owner-encrypted-email (<! (get-email {:district0x-emails/address original-owner}))]
      (when-let [to (validate-email owner-encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "Your auction received a new bid: " name)
                     :content (templates/on-new-bid offering name price)
                     :substitutions {:header (str name " auction")
                                     :button-title "See auction details"
                                     :button-href (templates/form-link offering)}
                     :on-success #(info "Success sending email" {:address original-owner} ::on-new-bid)
                     :on-error #(error "Error when sending email" {:error %} ::on-new-bid)
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})))))


(defn on-offering-changed [{:keys [:offering :version :event-type :extra-data] :as args}]
  (info info-text {:args args} ::on-offering-changed)
  (case (hex-to-utf8 @web3 event-type)
    "bid" (on-new-bid args)
    "finalize" (safe-go
                 (let [offering (<! (offering/get-offering offering))
                       {:keys [:offering/name
                               :offering/original-owner
                               :offering/price
                               :offering/winning-bidder]} offering
                       ]
                   (if winning-bidder
                     (on-auction-finalized offering original-owner winning-bidder name price)
                     (on-offering-bought offering original-owner name price))))
    "uninteresting event"))


(defn- dispatcher [callback]
  (fn [err {:keys [:latest-event? :args] :as ev}]
    (when latest-event?
      (if err
        (error "Emailer got error from blockchain event" {:error err} ::event-callback)
        (callback args)))))


(defn start [{:keys [:api-key :private-key :print-mode?] :as opts}]
  (when-not private-key
    (throw (js/Error. ":private-key is required to start emailer")))
  (let [callback-ids
        [(web3-events/register-callback! :offering-registry/offering-added (dispatcher on-offering-added))
         (web3-events/register-callback! :offering-registry/offering-changed (dispatcher on-offering-changed))]]
    (assoc opts :callback-ids callback-ids)))


(defn stop [emailer]
  (web3-events/unregister-callbacks! (:callback-ids @emailer)))