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
    [district0x.shared.utils :refer [prepend-address-zeros hex-to-utf8]]
    [medley.core :as medley]
    [mount.core :as mount :refer [defstate]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
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
               (= (string/lower-case owner) offering-address))))


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
        (update :bid/bidder (comp prepend-address-zeros (partial web3-utils/number-to-hex @web3)))
        (update :bid/value bn/number)
        (update :bid/datetime bn/number)
        (assoc :bid/offering offering)
        (->> (db/insert-bid!)))))


(defn on-offering-changed [err {{:keys [:event-type] :or {event-type "0x"} :as args} :args :as event}]
  (log/info info-text {:args args} ::on-offering-changed)
  (if (= "bid" (hex-to-utf8 @web3 event-type))
    (on-offering-bid err event)
    (safe-go
      (let [offering (<! (get-offering (:offering args)))]
        (if (and (:offering/valid-name? offering)
                 (:offering/normalized? offering))
          (db/upsert-offering! offering)
          (log/warn "Malformed name offering" offering ::on-offering-changed))))))


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


(defn dispatcher [callback]
  (fn [err event]
    (let [event (-> event
                    (medley/update-existing-in [:args :offering] string/lower-case)
                    (medley/update-existing-in [:args :owner] string/lower-case)
                    (medley/update-existing-in [:args :from] string/lower-case)
                    (medley/update-existing-in [:args :to] string/lower-case))]
      (callback err event))))


(defn start [opts]
  (safe-go
    (when-not (web3-eth/is-listening? @web3)
      (throw (js/Error. "Can't connect to Ethereum node")))
    (let [event-callbacks {:ens/new-owner on-ens-transfer
                           :ens/transfer on-ens-transfer
                           :offering-registry/offering-added on-offering-changed
                           :offering-registry/offering-changed on-offering-changed
                           :registrar/transfer on-registrar-transfer}
          callback-ids (doall (for [[event-key callback] event-callbacks]
                                (web3-events/register-callback! event-key (dispatcher callback))))]
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
