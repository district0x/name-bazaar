(ns name-bazaar.server.contracts-api.offering
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.shared.big-number :as bn]
    [district0x.server.state :as state]
    [district0x.utils :refer [zero-address?]]
    [name-bazaar.shared.utils :refer [parse-offering]]))

(defn get-offering [server-state contract-address]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (parse-offering contract-address res)])))
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :instant-buy-offering))
                                contract-address)
    :offering))