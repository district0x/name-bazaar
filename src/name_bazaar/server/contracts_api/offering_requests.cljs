(ns name-bazaar.server.contracts-api.offering-requests
  (:require
    [district.server.smart-contracts :refer [contract-call]]
    [name-bazaar.shared.utils :refer [parse-offering-request]]))

(defn on-request-added [& args]
  (apply contract-call :offering-requests :on-request-added args))

(defn on-round-changed [& args]
  (apply contract-call :offering-requests :on-round-changed args))

(defn add-request! [{:keys [:offering-request/name]} opts]
  (contract-call :offering-requests
                 :add-request
                 name
                 (merge {:gas 1000000} opts)))

(defn get-request [{:keys [:offering-request/node]}]
  (->> (contract-call :offering-requests :get-request node)
    (parse-offering-request node)))

(defn get-requesters [{:keys [:offering-request/node :offering-request/round]}]
  (contract-call :offering-requests :get-requesters node round))
