(ns name-bazaar.contracts-api.english-auction-offering
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [cljs-web3.core :as web3]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as u]))

(defn bid! [server-state {:keys [:contract-address :value-ether] :as opts}]
  (effects/logged-contract-call! server-state
                                 (web3-eth/contract-at (state/web3 server-state)
                                                       (:abi (state/contract :english-auction-offering))
                                                       contract-address)
                                 :bid
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)
                                         :value (when value-ether (web3/to-wei value-ether :ether))}
                                        opts)))