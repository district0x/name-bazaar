(ns name-bazaar.server.contracts-api.buy-now-offering
  (:require
    [district.server.smart-contracts :refer [contract-call]]))

(defn buy! [{:keys [:offering/address]} opts]
  (contract-call [:buy-now-offering address]
                 :buy
                 (merge {:gas 300000} opts)))

(defn reclaim-ownership! [contract-address opts]
  (contract-call [:buy-now-offering contract-address]
                 :reclaim-ownership
                 (merge {:gas 300000} opts)))

(defn set-settings! [{:keys [:offering/address :offering/price]} opts]
  (contract-call [:buy-now-offering address]
                 :set-settings
                 price
                 (merge {:gas 300000} opts)))

(defn set-offering-registry! [{:keys [:offering/address :offering/registry]} opts]
  (contract-call [:buy-now-offering address]
                 :set-offering-registry
                 registry
                 (merge {:gas 300000} opts)))
