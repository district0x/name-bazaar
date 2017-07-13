(ns name-bazaar.contracts-api.english-auction-offering-factory
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]))

(defn create-offering! [server-state {:keys [:name :start-price :end-time :extension-duration :min-bid-increase] :as opts}]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :english-auction-offering-factory)
                                 :create-offering
                                 name
                                 start-price
                                 end-time
                                 extension-duration
                                 min-bid-increase
                                 (merge {:gas 1000000
                                         :from (state/active-address server-state)}
                                        opts)))


