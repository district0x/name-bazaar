(ns name-bazaar.server.contracts-api.offering-requests
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.shared.big-number :as bn]
    [name-bazaar.shared.utils :refer [parse-offering-request]]))

(defn on-request-added [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-request-added args))

(defn on-round-changed [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-round-changed args))

(defn add-request! [server-state {:keys [:offering-request/name]} opts]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :offering-requests)
                                 :add-request
                                 name
                                 (merge {:gas 1000000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn get-request [server-state {:keys [:offering-request/node]}]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (parse-offering-request node res)])))
    (state/instance server-state :offering-requests)
    :get-request
    node))

(defn get-requesters [server-state {:keys [:offering-request/node :offering-request/round]}]
  (web3-eth-async/contract-call (state/instance server-state :offering-requests) :get-requesters node round))