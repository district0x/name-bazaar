(ns name-bazaar.server.db-sync
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [clojure.string :as string]
    [district0x.server.logging :as logging]
    [district0x.server.macros :refer-macros [gotry]]
    [district0x.server.state :as state]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :refer [prepend-address-zeros]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.db :as db]
    [name-bazaar.shared.utils :refer [offering-version->type]]
    [name-bazaar.server.contracts-api.mock-registrar :as registrar]))

(defonce event-filters (atom []))

(defn node-owner? [server-state offering-address {:keys [:offering/name :offering/node] :as offering}]
  (let [ch (chan)]
    (gotry
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
    (gotry
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
  (logging/info ::on-offering-changed "Handling blockchain event" {:args args})
  (gotry
    (let [offering (<! (get-offering-from-event server-state args))]
      (db/upsert-offering! (state/db server-state) offering))))

(defn on-offering-bid [server-state err {{:keys [:offering :version :extra-data] :as args} :args}]
  (logging/info ::on-offering-bid "Handling blockchain event" {:args args})
  (try 
    (-> (zipmap [:bid/bidder :bid/value :bid/datetime] extra-data)
        (update :bid/bidder (comp prepend-address-zeros web3/from-decimal))
        (update :bid/value bn/->number)
        (update :bid/datetime bn/->number)
        (assoc :bid/offering offering)
        (->> (db/insert-bid! (state/db server-state))))
    (catch :default e
      (logging/error ::on-offering-bid "Error handling blockchain event" {:error e}))))

(defn stop-watching-filters! []
  (doseq [filter @event-filters]
    (when filter
      (web3-eth/stop-watching! filter (fn [])))))

(defn on-request-added [server-state err {{:keys [:node :round :requesters-count] :as args} :args}]
  (logging/info ::on-request-added "Handling block event" {:args args})
  (db/upsert-offering-requests-rounds! (state/db server-state)
                                       {:offering-request/node node
                                        :offering-request/round (bn/->number round)
                                        :offering-request/requesters-count (bn/->number requesters-count)}))

(defn on-round-changed [server-state err {{:keys [:node :latest-round] :as args} :args}]
  (logging/info ::on-round-changed "Handling block event" {:args args})
  (gotry
    (let [latest-round (bn/->number latest-round)
          request (second (<! (offering-requests/get-request server-state {:offering-request/node node})))]
      (db/upsert-offering-requests! (state/db server-state)
                                    (-> request
                                        (assoc :offering-request/latest-round latest-round)))
      (when (= latest-round (:offering-request/latest-round request))
        ;; This is optimisation so we don't have to go through all on-request-added from block 0
        ;; We just save current count of latest round, because it's all we need. Don't need all history
        (on-request-added server-state nil {:args {:node node
                                                   :round latest-round
                                                   :requesters-count (:offering-request/requesters-count request)}})))))

(defn on-ens-new-owner [server-state err {{:keys [:node :owner] :as args} :args}]  
  (gotry
    (let [offering (second (<! (offering/get-offering server-state owner)))]
      (when offering
        (do
          (logging/info ::on-ens-new-owner "Handling blockchain event" {:args args})
          (let [owner? (<! (node-owner? server-state owner offering))]
            (db/set-offering-node-owner?! (state/db server-state) {:offering/address owner
                                                                   :offering/node-owner? owner?})))))))

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

           (offering-requests/on-round-changed server-state
                                               {}
                                               {:from-block 0 :to-block "latest"}
                                               (partial on-round-changed server-state))

           (offering-registry/on-offering-changed server-state
                                                  {:event-type "bid"}
                                                  {:from-block 0 :to-block "latest"}
                                                  (partial on-offering-bid server-state))]))

(defn stop-syncing! []
  (stop-watching-filters!)
  (reset! event-filters []))
