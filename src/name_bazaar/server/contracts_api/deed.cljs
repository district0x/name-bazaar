(ns name-bazaar.server.contracts-api.deed
  (:require
    [district.server.smart-contracts :refer [contract-call]]))

(defn owner [contract-address]
  (contract-call [:deed contract-address] :owner))

