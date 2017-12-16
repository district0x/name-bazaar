(ns name-bazaar.server.contracts-api.district0x-emails
  (:require
    [district.server.smart-contracts :refer [contract-call]]))

(defn get-email [{:keys [:district0x-emails/address]}]
  (contract-call :district0x-emails :get-email address))
