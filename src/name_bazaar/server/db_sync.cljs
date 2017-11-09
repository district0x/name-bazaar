(ns name-bazaar.server.db-sync
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan timeout]]
    [clojure.string :as string]
    [district0x.server.state :as state]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [prepend-address-zeros jsobj->clj]]
    [district0x.server.effects :as d0x-effects]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.db :as db]
    [name-bazaar.shared.utils :refer [offering-version->type]]
    [name-bazaar.server.contracts-api.registrar :as registrar]
    [taoensso.timbre :as logging :refer-macros [info warn error]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [district0x.server.macros :refer [gotry]]))

(defonce event-filters (atom []))

(defn node-owner? [offering-address {:keys [:offering/name :offering/node :offering/top-level-name? :offering/label]
                                     :as offering}]
  (let [ch (chan)]
    (gotry
      (let [[err ens-owner] (<! (ens/owner {:ens.record/node node}))
            offering-ens-owner? (= ens-owner offering-address)]
        (when err
          (error "Error getting ens owner " {:error err :offering offering} ::node-owner?))
        (>! ch
            (if top-level-name?
              (and offering-ens-owner?
                   (= (second (<! (registrar/entry-deed-owner {:ens.record/label label})))
                      offering-address))
              offering-ens-owner?))))
    ch))

(defn auction? [version]
  (= (offering-version->type version) :auction-offering))

(defn get-offering-from-event [event-args]
  (let [ch (chan)]
    (gotry
      (let [offering (second (<! (offering/get-offering (:offering event-args))))
            auction-offering (when (auction? (:version event-args))
                               (second (<! (auction-offering/get-auction-offering (:offering event-args)))))
            owner? (<! (node-owner? (:offering event-args) offering))
            offering (-> offering
                       (merge auction-offering)
                       (assoc :offering/node-owner? owner?))]
        (>! ch offering)))
    ch))

(defn on-offering-changed [err {:keys [:args]}]
  (info "Handling blockchain event" {:args args} ::on-offering-changed)
  (gotry
    (let [offering (<! (get-offering-from-event args))]
      (if (and (:offering/valid-name? offering)
               (:offering/normalized? offering))
        (db/upsert-offering! (state/db) offering)
        (warn [:MAILFORMED-NAME-OFFERING offering])))))

(defn on-offering-bid [err {{:keys [:offering :version :extra-data] :as args} :args}]
  (info "Handling blockchain event" {:args args} ::on-offering-bid)
  (try
    (-> (zipmap [:bid/bidder :bid/value :bid/datetime] extra-data)
      (update :bid/bidder (comp prepend-address-zeros web3/from-decimal))
      (update :bid/value bn/->number)
      (update :bid/datetime bn/->number)
      (assoc :bid/offering offering)
      (->> (db/insert-bid! (state/db))))
    (catch :default e
      (logging/error "Error handling blockchain event" {:error (jsobj->clj e)} ::on-offering-bid))))

(defn stop-watching-filters! []
  (doseq [filter @event-filters]
    (when filter
      (web3-eth/stop-watching! filter (fn [])))))

(defn on-request-added [err {{:keys [:node :round :requesters-count] :as args} :args}]
  (info "Handling blockchain event" {:args args} ::on-request-added)
  (db/upsert-offering-requests-rounds! (state/db)
                                       {:offering-request/node node
                                        :offering-request/round (bn/->number round)
                                        :offering-request/requesters-count (bn/->number requesters-count)}))

(defn on-round-changed [err {{:keys [:node :latest-round] :as args} :args}]
  (info "Handling blockchain event" {:args args} ::on-round-changed)
  (gotry
    (let [latest-round (bn/->number latest-round)
          request (second (<! (offering-requests/get-request {:offering-request/node node})))]
      (db/upsert-offering-requests! (state/db)
                                    (-> request
                                      (assoc :offering-request/latest-round latest-round)))
      (when (= latest-round (:offering-request/latest-round request))
        ;; This is optimisation so we don't have to go through all on-request-added from block 0
        ;; We just save current count of latest round, because it's all we need. Don't need all history
        (on-request-added nil {:args {:node node
                                      :round latest-round
                                      :requesters-count (:offering-request/requesters-count request)}})))))

(defn on-ens-transfer [err {{:keys [:node :owner] :as args} :args}]
  (gotry
    (let [offering (second (<! (offering/get-offering owner)))]
      (when offering
        (do
          (logging/info "Handling blockchain event" {:args args} ::on-ens-new-owner)
          (let [owner? (<! (node-owner? owner offering))]
            (db/set-offering-node-owner?! (state/db) {:offering/address owner
                                                      :offering/node-owner? owner?})))))))

(defn start-syncing! []
  (db/create-tables! (state/db))
  (stop-watching-filters!)
  (reset! event-filters
          [(offering-registry/on-offering-changed {} "latest" on-offering-changed)
           (offering-requests/on-request-added {} "latest" on-request-added)
           (ens/on-new-owner {} "latest" on-ens-transfer)
           (ens/on-transfer {} "latest" on-ens-transfer)
           (offering-registry/on-offering-added {} {:from-block 0 :to-block "latest"} on-offering-changed)
           (offering-requests/on-round-changed {} {:from-block 0 :to-block "latest"} on-round-changed)
           (offering-registry/on-offering-changed {:event-type "bid"} {:from-block 0 :to-block "latest"} on-offering-bid)]))

(defn stop-syncing! []
  (stop-watching-filters!)
  (reset! event-filters []))
