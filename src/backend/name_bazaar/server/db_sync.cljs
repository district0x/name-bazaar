(ns name-bazaar.server.db-sync
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.state :as state]
    [name-bazaar.contracts-api.english-auction-offering :as english-auction-offering]
    [name-bazaar.contracts-api.ens :as ens]
    [name-bazaar.contracts-api.offering :as offering]
    [name-bazaar.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.db :as db]
    [name-bazaar.shared.utils :refer [offering-type->kw]]
    [district0x.big-number :as bn])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce event-filters (atom {}))

(defn on-offering-added [server-state err {:keys [:args]}]
  (go
    (let [offering-ch (offering/get-offering server-state (:offering args))
          owner-ch (ens/owner server-state {:node (:node args)})
          english-auction-ch (when (= (offering-type->kw (:offering-type args)) :english-auction-offering)
                               (english-auction-offering/get-english-auction-offering server-state
                                                                                      (:offering args)))
          offering-data (cond-> (second (<! offering-ch))
                          true (assoc :offering/node-owner? (= (:offering args) (second (<! owner-ch))))
                          english-auction-ch (merge (second (<! english-auction-ch))))]

      (db/upsert-offering! (state/db server-state) offering-data))))

(defn on-offering-changed [server-state err {:keys [:args]}]
  (go
    (let [offering-ch (offering/get-offering server-state (:offering args))
          english-auction-ch (when (= (offering-type->kw (:offering-type args)) :english-auction-offering)
                               (english-auction-offering/get-english-auction-offering server-state
                                                                                      (:offering args)))
          offering-data (second (<! offering-ch))
          owner (second (<! (ens/owner server-state {:node (:offering/node offering-data)})))
          offering-data (cond-> offering-data
                          true (assoc :offering/node-owner? (= (:offering args) owner))
                          english-auction-ch (merge (second (<! english-auction-ch))))]
      (db/upsert-offering! (state/db server-state) offering-data))))

(defn stop-watching-filters! []
  (doseq [filter (vals @event-filters)]
    (when filter
      (web3-eth/stop-watching! filter (fn [])))))

(defn on-new-requests [server-state err {{:keys [:node]} :args}]
  (go
    (let [requests-count (second (<! (offering-requests/requests-count server-state {:offering-requests/node node})))]
      (db/upsert-offering-requests! (state/db server-state) {:offering-requests/node node
                                                             :offering-requests/count requests-count}))))

(defn on-request-added [server-state err {{:keys [:node :requests-count]} :args}]
  (db/upsert-offering-requests! (state/db server-state) {:offering-requests/node (print.foo/look node)
                                                         :offering-requests/count (bn/->number requests-count)}))

(defn start-syncing! [server-state]
  (db/create-tables! (state/db server-state))
  (stop-watching-filters!)
  (swap! event-filters
         merge
         {:on-offering-changed (offering-registry/on-offering-changed server-state
                                                                      {}
                                                                      "latest"
                                                                      (partial on-offering-changed server-state))

          :on-offering-added (offering-registry/on-offering-added server-state
                                                                  {}
                                                                  {:from-block 0 :to-block "latest"}
                                                                  (partial on-offering-added server-state))
          :on-new-requests (offering-requests/on-new-requests server-state
                                                              {}
                                                              {:from-block 0 :to-block "latest"}
                                                              (partial on-new-requests server-state))
          :on-request-added (offering-requests/on-request-added server-state
                                                                {}
                                                                "latest"
                                                                (partial on-request-added server-state))}))

(defn stop-syncing! []
  (stop-watching-filters!)
  (reset! event-filters {}))
