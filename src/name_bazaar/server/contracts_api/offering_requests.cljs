(ns name-bazaar.server.contracts-api.offering-requests
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.shared.big-number :as bn]
    [name-bazaar.shared.utils :refer [parse-offering-request]]))

(defn on-request-added [& args]
  (apply web3-eth/contract-call (state/instance :offering-requests) :on-request-added args))

(defn on-round-changed [& args]
  (apply web3-eth/contract-call (state/instance :offering-requests) :on-round-changed args))

(defn add-request! [{:keys [:offering-request/name]} opts]
  (logged-contract-call! (state/instance :offering-requests)
                                 :add-request
                                 name
                                 (merge {:gas 1000000
                                         :from (state/active-address)}
                                        opts)))

(defn get-request [{:keys [:offering-request/node]}]
  (queue-contract-call!
    (chan 1 (map (fn [[err res]]
                   [err (parse-offering-request node res)])))
    (state/instance :offering-requests)
    :get-request
    node))

(defn get-requesters [{:keys [:offering-request/node :offering-request/round]}]
  (queue-contract-call! (state/instance :offering-requests) :get-requesters node round))
