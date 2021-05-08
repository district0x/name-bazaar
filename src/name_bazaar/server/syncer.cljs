(ns name-bazaar.server.syncer
  (:require
    [bignumber.core :as bn]
    [cljs.core.async :refer [<! go]]
    [cljs-web3-next.core :as web3-core]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.utils :as web3-utils]
    [clojure.string :as string]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :as smart-contracts]
    [district.server.web3-events :as web3-events]
    [district.server.web3 :refer [ping-start ping-stop web3]]
    [district.shared.async-helpers :refer [promise-> safe-go extend-promises-as-channels!]]
    [district.shared.error-handling :refer [try-catch]]
    [district0x.shared.utils :refer [prepend-address-zeros]]
    [mount.core :as mount :refer [defstate]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.contracts-api.registrar :as registrar]
    [name-bazaar.server.db :as db]
    [name-bazaar.server.generator]
    [taoensso.timbre :as log]))

(extend-promises-as-channels!)

(def info-text "Handling blockchain event")
(def error-text "Error handling blockchain event")

(defn node-owner? [offering-address {:keys [:offering/node :offering/top-level-name? :offering/label]}]
  (promise-> (if top-level-name?
               (registrar/registration-owner {:ens.record/label label})
               (ens/owner {:ens.record/node node}))
             (fn [owner]
               (= owner offering-address))))


(defn get-offering [offering-address]
  ;; TODO: Why is this nil?
  (assert (not= offering-address nil) "Offering address shouldn't be nil")
  (promise-> (offering/get-offering offering-address)
             (fn [offering]
               (if (:offering/auction? offering)
                 (.then (auction-offering/get-auction-offering offering-address)
                        #(merge offering %))
                 offering))
             (fn [offering]
               (.then (node-owner? offering-address offering)
                      #(assoc offering :offering/node-owner? %)))))


(defn on-offering-bid [err {{:keys [:offering :version :extra-data] :as args} :args}]
  (log/info info-text {:args args} ::on-offering-bid)
  (safe-go
    (when-not (db/offering-exists? offering)
      (db/upsert-offering! (<! (get-offering offering))))
    (-> (zipmap [:bid/bidder :bid/value :bid/datetime] extra-data)
        (update :bid/bidder (comp prepend-address-zeros web3/from-decimal))
        (update :bid/value bn/number)
        (update :bid/datetime bn/number)
        (assoc :bid/offering offering)
        (->> (db/insert-bid!)))))


(defn on-request-added [err {{:keys [:node :round :requesters-count] :as args} :args}]
  (log/info info-text {:args args} ::on-request-added)
  (safe-go
    (db/upsert-offering-requests-rounds!
      {:offering-request/node node
       :offering-request/round (bn/number round)
       :offering-request/requesters-count (bn/number requesters-count)})))


(defn on-round-changed [err {{:keys [:node :latest-round] :as args} :args}]
  (log/info info-text {:args args} ::on-round-changed)
  (safe-go
    (let [latest-round (bn/number latest-round)
          request (<! (offering-requests/get-request {:offering-request/node node}))]
      (db/upsert-offering-requests! (assoc request :offering-request/latest-round latest-round))
      (when (= latest-round (:offering-request/latest-round request))
        ;; This is optimisation so we don't have to go through all on-request-added from block 0
        ;; We just save current count of latest round, because it's all we need. Don't need all history
        (on-request-added nil {:args {:node node
                                      :round latest-round
                                      :requesters-count (:offering-request/requesters-count request)}})))))


(defn on-ens-transfer [err {{:keys [:node :owner] :as args} :args}]
  (safe-go
    (when (db/offering-exists? owner)
      (let [offering (<! (offering/get-offering owner))]
        (log/info info-text {:args args} ::on-ens-new-owner)
        (db/set-offering-node-owner?! {:offering/address owner
                                       :offering/node-owner? (<! (node-owner? owner offering))})))))


(defn on-registrar-transfer [err {{:keys [:from :to :id] :as args} :args}]
  (safe-go
    (when (db/offering-exists? to)
      (let [offering (<! (offering/get-offering to))
            node-owner? (<! (node-owner? to offering))]
        (log/info info-text {:args args} ::on-registrar-new-owner)
        (db/set-offering-node-owner?! {:offering/address to
                                       :offering/node-owner? node-owner?})))))

(defn start [opts]
  (safe-go
    (when-not (web3-eth/is-listening? @web3)
      (throw (js/Error. "Can't connect to Ethereum node")))
    (let [event-callbacks {:ens/new-owner on-ens-transfer
                           :ens/transfer on-ens-transfer
                           :offering-registry/offering-added on-offering-changed
                           :offering-registry/offering-changed on-offering-changed
                           :offering-requests/request-added on-request-added
                           :offering-requests/round-changed on-round-changed
                           :registrar/transfer on-registrar-transfer}
          callback-ids (doall (for [[event-key callback] event-callbacks]
                                (web3-events/register-callback! event-key callback)))]
      (web3-events/register-after-past-events-dispatched-callback! (fn []
                                                                     (log/warn "Syncing past events finished")
                                                                     (ping-start {:ping-interval 10000})))
      (assoc opts :callback-ids callback-ids))))


(defn stop [syncer]
  (ping-stop)
  (web3-events/unregister-callbacks! (:callback-ids @syncer)))


(defstate ^{:on-reload :noop} syncer
          :start (start (merge (:syncer @config)
                               (:syncer (mount/args))))
          :stop (stop syncer))
