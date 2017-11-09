(ns name-bazaar.server.contracts-api.used-by-factories
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]))

(defn set-factories! [{:keys [:contract-key] :as opts}]
  (logged-contract-call! (state/instance contract-key)
                         :set-factories
                         [(state/contract-address :buy-now-offering-factory)
                          (state/contract-address :auction-offering-factory)]
                         true
                         (merge
                           {:gas 100000
                            :from (state/active-address)}
                           (select-keys opts [:from :gas-price :gas :value :data]))))
