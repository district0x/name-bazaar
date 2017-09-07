(ns name-bazaar.server.db-sync
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.shared.big-number :as bn]
    [district0x.server.state :as state]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.db :as db]
    [name-bazaar.shared.utils :refer [offering-version->type]]
    [clojure.string :as string]
    [name-bazaar.server.contracts-api.mock-registrar :as registrar])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce event-filters (atom []))

(defn node-owner? [server-state offering-address {:keys [:offering/name :offering/node] :as offering}]
  (let [ch (chan)]
    (go
      (let [ens-owner-ch (ens/owner server-state {:ens.record/node node})
            split-name (string/split name ".")]
        (>! ch
            (if (and (= (count split-name) 2)               ;; For TLD .eth names we must also verify deed ownership
                     (= (last split-name) "eth"))
              (and (= (second (<! (registrar/entry-deed-owner server-state {:ens.record/label (first split-name)})))
                      offering-address)
                   (= (second (<! ens-owner-ch))
                      offering-address))
              (= (second (<! ens-owner-ch))                 ;; For other names just basic ENS ownership check
                 offering-address)))))
    ch))

(defn auction? [version]
  (= (offering-version->type version) :auction-offering))

(defn get-offering-from-event [server-state event-args]
  (let [ch (chan)]
    (go
      (let [offering (second (<! (offering/get-offering server-state (:offering event-args))))
            auction-offering (when (auction? (:version event-args))
                               (second (<! (auction-offering/get-auction-offering server-state
                                                                                  (:offering event-args)))))
            owner? (<! (node-owner? server-state (:offering event-args) offering))
            offering (-> offering
                       (merge auction-offering)
                       (assoc :offering/node-owner? owner?))]
        (>! ch offering)))
    ch))

(defn on-offering-changed [server-state err {:keys [:args]}]
  (go
    (let [offering (<! (get-offering-from-event server-state args))]
      (db/upsert-offering! (state/db server-state) offering))))

(defn on-offering-bid [server-state err {{:keys [:offering :version :bidder :value]} :args}]
  (db/insert-bid! (state/db server-state) {:bid/bidder bidder
                                           :bid/value (bn/->number value)
                                           :bid/offering offering}))

(defn stop-watching-filters! []
  (doseq [filter @event-filters]
    (when filter
      (web3-eth/stop-watching! filter (fn [])))))

(defn on-new-requests [server-state err {{:keys [:node :name]} :args}]
  (go
    (let [{:keys [:offering-request/requesters-count]}
          (second (<! (offering-requests/get-request server-state {:offering-request/node node})))]
      (db/upsert-offering-requests! (state/db server-state) {:offering-request/node node
                                                             :offering-request/name name
                                                             :offering-request/requesters-count requesters-count}))))

(defn on-request-added [server-state err {{:keys [:node :name :requesters-count]} :args}]
  (db/upsert-offering-requests! (state/db server-state) {:offering-request/node node
                                                         :offering-request/name name
                                                         :offering-request/requesters-count (bn/->number requesters-count)}))

(defn on-ens-new-owner [server-state err {{:keys [:node :owner]} :args}]
  (go
    (let [offering (second (<! (offering/get-offering server-state owner)))]
      (when offering
        (let [owner? (<! (node-owner? server-state owner offering))]
          (db/set-offering-node-owner?! (state/db server-state) {:offering/address owner
                                                                 :offering/node-owner? owner?}))))))

(defn start-syncing! [server-state]
  (db/create-tables! (state/db server-state))
  (stop-watching-filters!)
  (reset! event-filters
          [(offering-registry/on-offering-changed server-state
                                                  {}
                                                  "latest"
                                                  (partial on-offering-changed server-state))

           (offering-requests/on-request-added server-state
                                               {}
                                               "latest"
                                               (partial on-request-added server-state))
           (ens/on-new-owner server-state
                             {}
                             "latest"
                             (partial on-ens-new-owner server-state))

           (offering-registry/on-offering-added server-state
                                                {}
                                                {:from-block 0 :to-block "latest"}
                                                (partial on-offering-changed server-state))

           (offering-requests/on-new-requests server-state
                                              {}
                                              {:from-block 0 :to-block "latest"}
                                              (partial on-new-requests server-state))

           (offering-registry/on-offering-bid server-state
                                              {}
                                              {:from-block 0 :to-block "latest"}
                                              (partial on-offering-bid server-state))]))

(defn stop-syncing! []
  (stop-watching-filters!)
  (reset! event-filters []))
