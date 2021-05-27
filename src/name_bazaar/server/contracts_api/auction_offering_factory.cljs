(ns name-bazaar.server.contracts-api.auction-offering-factory
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send]]))

(defn create-offering! [args opts]
  (contract-send :auction-offering-factory
                 :create-offering
                 ((juxt :offering/name
                        :offering/price
                        :auction-offering/end-time
                        :auction-offering/extension-duration
                        :auction-offering/min-bid-increase)
                  args)
                 (merge {:gas 1000000}
                        opts)))

(defn ens []
  (contract-call :auction-offering-factory :ens))

(defn root-node []
  (contract-call :auction-offering-factory :root-node))

(defn offering-registry []
  (contract-call :auction-offering-factory :offering-registry))
