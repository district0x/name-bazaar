(ns name-bazaar.server.contracts-api.offering-requests
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send]]
    [district.shared.async-helpers :refer [promise->]]
    [name-bazaar.shared.utils :refer [parse-offering-request]]))

(defn add-request! [{:keys [:offering-request/name]} opts]
  (contract-send :offering-requests
                 :add-request
                 [name]
                 (merge {:gas 1000000} opts)))

(defn get-request [{:keys [:offering-request/node]}]
  (promise-> (contract-call :offering-requests :get-request [node])
             #(parse-offering-request node %)))

(defn get-requesters [{:keys [:offering-request/node :offering-request/round]}]
  (contract-call :offering-requests :get-requesters [node round]))
