(ns name-bazaar.server.contracts-api.offering-requests
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.shared.big-number :as bn]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    ))

(defn on-request-added [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-request-added args))

(defn on-requests-cleared [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-requests-cleared args))

(defn on-new-requests [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-new-requests args))

(defn add-request! [server-state {:keys [:offering-request/name]} opts]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :offering-requests)
                                 :add-request
                                 name
                                 (merge {:gas 1000000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn requests-counts [server-state {:keys [:offering-requests/nodes]}]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (map bn/->number res)])))
    (state/instance server-state :offering-requests)
    :get-requests-counts
    nodes))