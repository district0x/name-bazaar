(ns name-bazaar.server.contracts-api.auction-offering-factory
  (:require
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [cljs-web3.async.eth :as web3-eth-async]))

(defn create-offering! [server-state args opts]
  (apply effects/logged-contract-call!
         server-state
         (state/instance server-state :auction-offering-factory)
         :create-offering
         (concat
           ((juxt :offering/name
                  :offering/price
                  :auction-offering/end-time
                  :auction-offering/extension-duration
                  :auction-offering/min-bid-increase)
             args)
           [(merge {:gas 1000000
                    :from (state/active-address server-state)}
                   opts)])))


(defn ens [server-state]
  (web3-eth-async/contract-call (state/instance server-state :auction-offering-factory) :ens))

(defn root-node [server-state]
  (web3-eth-async/contract-call (state/instance server-state :auction-offering-factory) :root-node))

(defn offering-registry [server-state]
  (web3-eth-async/contract-call (state/instance server-state :auction-offering-factory) :offering-registry))

(defn offering-requests [server-state]
  (web3-eth-async/contract-call (state/instance server-state :auction-offering-factory) :offering-requests))
