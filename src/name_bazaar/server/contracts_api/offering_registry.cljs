(ns name-bazaar.server.contracts-api.offering-registry
  (:require
    [district.server.smart-contracts.core :refer [contract-call contract-event-in-tx]]))

(defn on-offering-added-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash :offering-registry :on-offering-added args))

(defn on-offering-added [& args]
  (apply contract-call :offering-registry :on-offering-added args))

(defn on-offering-changed [& args]
  (apply contract-call :offering-registry :on-offering-changed args))

(defn on-offering-bid [& args]
  (apply contract-call :offering-registry :on-offering-bid args))

(defn emergency-pause! [opts]
  (contract-call :offering-registry :emergency-pause (merge {:gas 300000} opts)))

(defn emergency-release! [opts]
  (contract-call :offering-registry :emergency-release (merge {:gas 300000} opts)))
