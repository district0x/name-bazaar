(ns name-bazaar.server.contracts-api.offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.shared.big-number :as bn]
    [district0x.server.state :as state]
    [name-bazaar.shared.utils :refer [parse-offering]]))

(defn get-offering [server-state contract-address]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (parse-offering contract-address res)])))
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :buy-now-offering))
                                contract-address)
    :offering))

(defn emergency-multisig [server-state contract-address]
  (web3-eth-async/contract-call
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :buy-now-offering))
                                contract-address)
    :emergency-multisig))

(defn ens [server-state contract-address]
  (web3-eth-async/contract-call
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :buy-now-offering))
                                contract-address)
    :ens))

(defn offering-registry [server-state contract-address]
  (web3-eth-async/contract-call
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :buy-now-offering))
                                contract-address)
    :offering-registry))

