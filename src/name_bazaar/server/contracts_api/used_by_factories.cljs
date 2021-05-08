(ns name-bazaar.server.contracts-api.used-by-factories
  (:require
    [district.server.smart-contracts :refer [contract-send contract-address]]))

(defn set-factories! [{:keys [:contract-key] :as opts}]
  (contract-send contract-key
                 :set-factories
                 [[(contract-address :buy-now-offering-factory)
                   (contract-address :auction-offering-factory)]
                  true]
                 (merge
                   {:gas 100000}
                   (select-keys opts [:from :gas-price :gas :value :data]))))
