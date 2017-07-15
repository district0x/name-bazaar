(ns name-bazaar.contracts-api.english-auction-offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs.core.async :refer [<! >! chan]]
    [cljs-web3.core :as web3]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as u]
    [district0x.big-number :as bn]))

(def english-auction-offering-props [:english-auction-offering/end-time :english-auction-offering/extension-duration
                                     :english-auction-offering/min-bid-increase :english-auction-offering/winning-bidder])

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
                   [err (when res
                          (-> (zipmap english-auction-offering-props res)
                            (update :english-auction-offering/end-time bn/->number)
                            (update :english-auction-offering/extension-duration bn/->number)
                            (update :english-auction-offering/min-bid-increase bn/->number)))])))
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :english-auction-offering))
                                contract-address)
    :english-auction-offering))