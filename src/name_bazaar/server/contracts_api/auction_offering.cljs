(ns name-bazaar.server.contracts-api.auction-offering
  (:require
    [district.server.smart-contracts :refer [contract-call]]
    [name-bazaar.shared.utils :refer [parse-auction-offering]]))

(defn bid! [{:keys [:offering/address]} opts]
  (contract-call [:auction-offering address]
                 :bid
                 (merge {:gas 300000} opts)))

(defn get-auction-offering [contract-address]
  (parse-auction-offering
    (contract-call [:auction-offering contract-address] :auction-offering)))

(defn finalize! [contract-address opts]
  (contract-call [:auction-offering contract-address]
                 :finalize
                 (merge {:gas 300000} opts)))

(defn withdraw! [{:keys [:offering :address]} {:keys [:from] :as opts}]
  (contract-call [:auction-offering offering]
                 :withdraw
                 address
                 (merge {:gas 300000})))

(defn reclaim-ownership! [contract-address opts]
  (contract-call [:auction-offering contract-address]
                 :reclaim-ownership
                 (merge {:gas 300000}
                        opts)))

(defn set-settings! [{:keys [:offering/address
                             :offering/price
                             :auction-offering/end-time
                             :auction-offering/extension-duration
                             :auction-offering/min-bid-increase]} opts]

  (contract-call [:auction-offering address]
                 :set-settings
                 price
                 end-time
                 extension-duration
                 min-bid-increase
                 (merge {:gas 300000}
                        opts)))
