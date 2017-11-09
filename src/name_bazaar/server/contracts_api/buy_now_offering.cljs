(ns name-bazaar.server.contracts-api.buy-now-offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.async.eth :as web3-eth]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [>! chan]]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn buy! [{:keys [:offering/address]} opts]
  (logged-contract-call! (web3-eth/contract-at (state/web3)
                                               (:abi (state/contract :buy-now-offering))
                                               address)
                         :buy
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn reclaim-ownership! [contract-address opts]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :buy-now-offering))
                                                     contract-address)
                         :reclaim-ownership
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn set-settings! [{:keys [:offering/address :offering/price]} opts]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :buy-now-offering))
                                                     address)
                         :set-settings
                         price
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn set-offering-registry! [{:keys [:offering/address :offering/registry]} opts]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :buy-now-offering))
                                                     address)
                         :set-offering-registry
                         registry
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))
