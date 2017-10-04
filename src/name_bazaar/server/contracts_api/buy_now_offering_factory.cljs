(ns name-bazaar.server.contracts-api.buy-now-offering-factory
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [cljs-web3.async.eth :as web3-eth-async]))

(defn create-offering! [server-state {:keys [:offering/name :offering/price]} opts]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :buy-now-offering-factory)
                                 :create-offering
                                 name
                                 price
                                 (merge {:gas 1000000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn registrar [server-state]
  (web3-eth-async/contract-call (state/instance server-state :buy-now-offering-factory) :registrar))

(defn offering-registry [server-state]
  (web3-eth-async/contract-call (state/instance server-state :buy-now-offering-factory) :offering-registry))

(defn offering-requests [server-state]
  (web3-eth-async/contract-call (state/instance server-state :buy-now-offering-factory) :offering-requests))

(defn emergency-multisig [server-state]
  (web3-eth-async/contract-call (state/instance server-state :buy-now-offering-factory) :emergency-multisig))
