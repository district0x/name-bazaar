(ns name-bazaar.server.contracts-api.auction-offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [district0x.shared.big-number :as bn]
    [name-bazaar.shared.utils :refer [parse-auction-offering]]))

(defn bid! [server-state {:keys [:offering/address]} {:keys [:value-ether] :as opts}]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :auction-offering))
                                                             address)
                                 :bid
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)
                                         :value (when value-ether (web3/to-wei value-ether :ether))}
                                        opts)))

(defn get-auction-offering [server-state contract-address]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (parse-auction-offering res)])))
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :auction-offering))
                                contract-address)
    :auction-offering))

(defn finalize! [server-state {:keys [:offering/address]} {:keys [:value-ether] :as opts}]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract server-state :auction-offering))
                                                             address)
                                 :finalize
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)
                                         :value (when value-ether (web3/to-wei value-ether :ether))}
                                        opts)))

(defn withdraw! [server-state {:keys [:offering
                                      :address]} {:keys [:from] :as opts}]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state :auction-offering))
                                                             offering)
                                 :withdraw
                                 address
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn reclaim-ownership! [server-state contract-address opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :auction-offering))
                                                             contract-address)
                                 :reclaimOwnership
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

;; TODO
#_(defn unregister! [server-state contract-address opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :auction-offering))
                                                             contract-address)
                                 :unregister
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn set-settings! [server-state {:keys [:offering/address
                                          :offering/price
                                          :auction-offering/end-time
                                          :auction-offering/extension-duration
                                          :auction-offering/min-bid-increase]} opts]
  
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :auction-offering))
                                                             address)
                                 :setSettings
                                 price
                                 end-time
                                 extension-duration
                                 min-bid-increase
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))
