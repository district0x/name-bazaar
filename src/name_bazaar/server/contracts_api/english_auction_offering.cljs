(ns name-bazaar.server.contracts-api.english-auction-offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [district0x.shared.big-number :as bn]
    [name-bazaar.shared.utils :refer [parse-english-auction-offering]]))

(defn bid! [server-state {:keys [:contract-address :value-ether] :as opts}]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract server-state :english-auction-offering))
                                                             contract-address)
                                 :bid
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)
                                         :value (when value-ether (web3/to-wei value-ether :ether))}
                                        opts)))

(defn get-english-auction-offering [server-state contract-address]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (parse-english-auction-offering res)])))
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :english-auction-offering))
                                contract-address)
    :english-auction-offering))