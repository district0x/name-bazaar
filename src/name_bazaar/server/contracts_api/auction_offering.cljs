(ns name-bazaar.server.contracts-api.auction-offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [district0x.shared.big-number :as bn]
    [name-bazaar.shared.utils :refer [parse-auction-offering]]))

(defn bid! [{:keys [:offering/address]} opts]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :auction-offering))
                                                     address)
                         :bid
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn get-auction-offering [contract-address]
  (queue-contract-call!
    (chan 1 (map (fn [[err res]]
                   [err (parse-auction-offering res)])))
    (web3-eth-async/contract-at (state/web3)
                                (:abi (state/contract :auction-offering))
                                contract-address)
    :auction-offering))

(defn finalize! [{:keys [:offering/address]} opts]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :auction-offering))
                                                     address)
                         :finalize
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn withdraw! [{:keys [:offering :address]} {:keys [:from] :as opts}]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :auction-offering))
                                                     offering)
                         :withdraw
                         address
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn reclaim-ownership! [contract-address opts]
  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :auction-offering))
                                                     contract-address)
                         :reclaim-ownership
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn set-settings! [{:keys [:offering/address
                             :offering/price
                             :auction-offering/end-time
                             :auction-offering/extension-duration
                             :auction-offering/min-bid-increase]} opts]

  (logged-contract-call! (web3-eth-async/contract-at (state/web3)
                                                     (:abi (state/contract :auction-offering))
                                                     address)
                         :set-settings
                         price
                         end-time
                         extension-duration
                         min-bid-increase
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))
