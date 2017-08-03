(ns name-bazaar.server.contracts-api.instant-buy-offering-factory
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]))

(defn create-offering! [server-state {:keys [:offering/name :offering/price]} opts]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :instant-buy-offering-factory)
                                 :create-offering
                                 name
                                 price
                                 (merge {:gas 1000000
                                         :from (state/active-address server-state)}
                                        opts)))
