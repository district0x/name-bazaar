(ns name-bazaar.server.db-sync
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.shared.big-number :as bn]
    [district0x.server.state :as state]
    [name-bazaar.server.contracts-api.english-auction-offering :as english-auction-offering]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.db :as db]
    [name-bazaar.shared.utils :refer [offering-version->type]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce event-filters (atom []))

(defn on-offering-added [server-state err {:keys [:args]}]
  (go
    (let [offering-ch (offering/get-offering server-state (:offering args))
          owner-ch (ens/owner server-state {:node (:node args)})
          english-auction-ch (when (= (offering-version->type (:version args)) :english-auction-offering)
                               (english-auction-offering/get-english-auction-offering server-state
                                                                                      (:offering args)))
          offering-data (cond-> (second (<! offering-ch))
                          true (assoc :offering/node-owner? (= (:offering args) (second (<! owner-ch))))
                          english-auction-ch (merge (second (<! english-auction-ch))))]

      (db/upsert-offering! (state/db server-state) offering-data))))

(defn on-offering-changed [server-state err {:keys [:args]}]
  (go
    (let [offering-ch (offering/get-offering server-state (:offering args))
          english-auction-ch (when (= (offering-version->type (:version args)) :english-auction-offering)
                               (english-auction-offering/get-english-auction-offering server-state
                                                                                      (:offering args)))
          offering-data (second (<! offering-ch))
          owner (second (<! (ens/owner server-state {:node (:offering/node offering-data)})))
          offering-data (cond-> offering-data
                          true (assoc :offering/node-owner? (= (:offering args) owner))
                          english-auction-ch (merge (second (<! english-auction-ch))))]
      (db/upsert-offering! (state/db server-state) offering-data))))

(defn stop-watching-filters! []
  (doseq [filter @event-filters]
    (when filter
      (web3-eth/stop-watching! filter (fn [])))))

(defn on-new-requests [server-state err {{:keys [:node :name]} :args}]
  (go
    (let [requests-count (first (second (<! (offering-requests/requests-counts server-state {:offering-requests/nodes [node]}))))]
      (db/upsert-offering-requests! (state/db server-state) {:offering-request/node node
                                                             :offering-request/name name
                                                             :offering-request/requesters-count requests-count}))))

(defn on-request-added [server-state err {{:keys [:node :name :requests-count]} :args}]
  (db/upsert-offering-requests! (state/db server-state) {:offering-request/node node
                                                         :offering-request/name name
                                                         :offering-request/requesters-count (bn/->number requests-count)}))

(defn on-ens-transfer [server-state err {{:keys [:node :owner]} :args}]
  (db/set-offering-node-owner?! (state/db server-state) {:offering/address owner
                                                         :offering/node-owner? true}))

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

           (ens/on-transfer server-state
                            {}
                            "latest"
                            (partial on-ens-transfer server-state))

           (offering-registry/on-offering-added server-state
                                                {}
                                                {:from-block 0 :to-block "latest"}
                                                (partial on-offering-added server-state))

           (offering-requests/on-new-requests server-state
                                              {}
                                              {:from-block 0 :to-block "latest"}
                                              (partial on-new-requests server-state))]))

(defn stop-syncing! []
  (stop-watching-filters!)
  (reset! event-filters []))
