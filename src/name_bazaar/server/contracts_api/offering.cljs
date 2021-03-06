(ns name-bazaar.server.contracts-api.offering
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send]]
    [district.shared.async-helpers :refer [promise->]]
    [name-bazaar.shared.utils :refer [parse-offering]]))

(defn get-offering [contract-address]
  (promise-> (contract-call [:buy-now-offering contract-address] :offering)
             #(parse-offering contract-address %)))

(defn emergency-multisig [contract-address]
  (contract-call [:buy-now-offering contract-address] :emergency-multisig))

(defn ens [contract-address]
  (contract-call [:buy-now-offering contract-address] :ens))

(defn offering-registry [contract-address]
  (contract-call [:buy-now-offering contract-address] :offering-registry))

(defn unregister! [contract-address offering-type opts]
  (contract-send [:buy-now-offering contract-address]
                 :unregister
                 []
                 (merge {:gas 300000} opts)))
