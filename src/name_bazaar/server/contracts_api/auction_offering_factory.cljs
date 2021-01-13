(ns name-bazaar.server.contracts-api.auction-offering-factory
  (:require
    [district.server.smart-contracts :refer [contract-call]]))

(defn create-offering! [args opts]
  (apply contract-call :auction-offering-factory
         :create-offering
         (concat
           ((juxt :offering/name
                  :offering/price
                  :auction-offering/end-time
                  :auction-offering/extension-duration
                  :auction-offering/min-bid-increase)
            args)
           [(merge {:gas 1000000}
                   opts)])))


(defn ens []
  (contract-call :auction-offering-factory :ens))

(defn root-node []
  (contract-call :auction-offering-factory :root-node))

(defn offering-registry []
  (contract-call :auction-offering-factory :offering-registry))

(defn offering-requests []
  (contract-call :auction-offering-factory :offering-requests))
