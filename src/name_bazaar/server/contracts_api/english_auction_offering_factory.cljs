(ns name-bazaar.server.contracts-api.english-auction-offering-factory
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]))

(defn create-offering! [server-state args opts]
  (apply effects/logged-contract-call!
         server-state
         (state/instance server-state :english-auction-offering-factory)
         :create-offering
         (concat
           ((juxt :offering/name
                  :offering/price
                  :english-auction-offering/end-time
                  :english-auction-offering/extension-duration
                  :english-auction-offering/min-bid-increase)
             args)
           [(merge {:gas 1000000
                    :from (state/active-address server-state)}
                   opts)])))


