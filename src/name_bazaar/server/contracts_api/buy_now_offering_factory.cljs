(ns name-bazaar.server.contracts-api.buy-now-offering-factory
  (:require
    [district.server.smart-contracts :refer [contract-call]]))

(defn create-offering! [{:keys [:offering/name :offering/price]} opts]
  (contract-call :buy-now-offering-factory
                 :create-offering
                 name
                 price
                 (merge {:gas 1000000} opts)))

(defn ens []
  (contract-call :buy-now-offering-factory :ens))

(defn root-node []
  (contract-call :buy-now-offering-factory :root-node))

(defn offering-registry []
  (contract-call :buy-now-offering-factory :offering-registry))

(defn offering-requests []
  (contract-call :buy-now-offering-factory :offering-requests))
