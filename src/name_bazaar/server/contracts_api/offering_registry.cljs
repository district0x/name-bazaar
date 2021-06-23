(ns name-bazaar.server.contracts-api.offering-registry
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send contract-event-in-tx]]))

(defn on-offering-added-in-tx [receipt]
  (contract-event-in-tx :offering-registry :on-offering-added receipt))

(defn emergency-pause! [opts]
  (contract-send :offering-registry
                 :emergency-pause
                 []
                 (merge {:gas 300000} opts)))

(defn emergency-release! [opts]
  (contract-send :offering-registry
                 :emergency-release
                 []
                 (merge {:gas 300000} opts)))

(defn change-offering-fees! [fee opts]
  (contract-send :offering-registry
                 :change-offering-fee
                 [fee]
                 (merge {:gas 300000} opts)))
