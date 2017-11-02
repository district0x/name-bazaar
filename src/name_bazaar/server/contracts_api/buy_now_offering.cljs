(ns name-bazaar.server.contracts-api.buy-now-offering
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [cljs-web3.core :as web3]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs.core.async :refer [>! chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn buy! [server-state {:keys [:offering/address]} {:keys [:value-ether] :as opts}]
  (effects/logged-contract-call! server-state
                                 (web3-eth/contract-at (state/web3 server-state)
                                                       (:abi (state/contract server-state :buy-now-offering))
                                                       address)
                                 :buy
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)
                                         :value (when value-ether (web3/to-wei value-ether :ether))}
                                        opts)))

(defn reclaim-ownership! [server-state contract-address opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :buy-now-offering))
                                                             contract-address)
                                 :reclaimOwnership
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn unregister! [server-state contract-address opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :buy-now-offering))
                                                             contract-address)
                                 :unregister
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn set-settings! [server-state {:keys [:offering/address
                                          :offering/price]} opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :buy-now-offering))
                                                             address)
                                 :setSettings
                                 price
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn set-offering-registry! [server-state {:keys [:offering/address
                                                   :offering/registry]} opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :buy-now-offering))
                                                             address)
                                 :setOfferingRegistry
                                 registry
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))
