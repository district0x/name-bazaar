(ns name-bazaar.contracts-api.offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.big-number :as bn]
    [district0x.server.state :as state]
    [district0x.utils :refer [zero-address?]]))

(def offering-props [:offering/offering-registry :offering/ens :offering/node :offering/name :offering/original-owner
                     :offering/emergency-multisig :offering/type :offering/created-on :offering/new-owner :offering/price])

(defn get-offering [server-state contract-address]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (when res
                          (-> (zipmap offering-props res)
                            (assoc :offering/address contract-address)
                            (update :offering/type bn/->number)
                            (update :offering/price bn/->number)
                            (update :offering/created-on bn/->number)
                            (update :offering/new-owner #(when-not (zero-address? %)))))])))
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :offering))
                                contract-address)
    :offering))