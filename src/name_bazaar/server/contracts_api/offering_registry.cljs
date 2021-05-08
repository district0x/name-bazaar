(ns name-bazaar.server.contracts-api.offering-registry
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send contract-event-in-tx]]))

(defn on-offering-added-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash :offering-registry :on-offering-added args))

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
