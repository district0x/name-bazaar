(ns name-bazaar.server.contracts-api.offering
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as effects]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.shared.big-number :as bn]
    [name-bazaar.shared.utils :refer [parse-offering]]))

(defn get-offering [contract-address]
  (queue-contract-call!
    (chan 1 (map (fn [[err res]]
                   [err (parse-offering contract-address res)])))
    (web3-eth/contract-at (state/web3)
                          (:abi (state/contract :buy-now-offering))
                          contract-address)
    :offering))

(defn emergency-multisig [contract-address]
  (queue-contract-call!
    (web3-eth/contract-at (state/web3)
                          (:abi (state/contract :buy-now-offering))
                          contract-address)
    :emergency-multisig))

(defn ens [contract-address]
  (queue-contract-call!
    (web3-eth/contract-at (state/web3)
                          (:abi (state/contract :buy-now-offering))
                          contract-address)
    :ens))

(defn offering-registry [contract-address]
  (queue-contract-call!
    (web3-eth/contract-at (state/web3)
                          (:abi (state/contract :buy-now-offering))
                          contract-address)
    :offering-registry))

(defn unregister! [contract-address offering-type opts]
  (effects/logged-contract-call! (web3-eth/contract-at (state/web3)
                                                       (:abi (state/contract offering-type))
                                                       contract-address)
                                 :unregister
                                 (merge {:gas 300000
                                         :from (state/active-address)}
                                        opts)))
