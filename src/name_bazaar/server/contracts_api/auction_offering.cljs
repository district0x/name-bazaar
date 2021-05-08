(ns name-bazaar.server.contracts-api.auction-offering
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send]]
    [district.shared.async-helpers :refer [promise->]]
    [name-bazaar.shared.utils :refer [parse-auction-offering]]))

(defn bid! [{:keys [:offering/address]} opts]
  (contract-send [:auction-offering address]
                 :bid
                 []
                 (merge {:gas 300000} opts)))

(defn get-auction-offering [contract-address]
  (promise-> (contract-call [:auction-offering contract-address] :auction-offering)
             #(parse-auction-offering %)))

(defn finalize! [contract-address opts]
  (contract-send [:auction-offering contract-address]
                 :finalize
                 []
                 (merge {:gas 300000} opts)))

(defn withdraw! [{:keys [:offering :address]} {:keys [:from] :as opts}]
  (contract-send [:auction-offering offering]
                 :withdraw
                 [address]
                 (merge {:gas 300000})))

(defn reclaim-ownership! [contract-address opts]
  (contract-send [:auction-offering contract-address]
                 :reclaim-ownership
                 []
                 (merge {:gas 300000}
                        opts)))

(defn set-settings! [{:keys [:offering/address
                             :offering/price
                             :auction-offering/end-time
                             :auction-offering/extension-duration
                             :auction-offering/min-bid-increase]} opts]

  (contract-send [:auction-offering address]
                 :set-settings
                 [price end-time extension-duration min-bid-increase]
                 (merge {:gas 300000}
                        opts)))
