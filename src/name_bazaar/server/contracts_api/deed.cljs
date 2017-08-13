(ns name-bazaar.server.contracts-api.deed
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]))

(defn owner [server-state contract-address & [opts]]
  (web3-eth-async/contract-call
    (web3-eth-async/contract-at (state/web3 server-state)
                                (:abi (state/contract server-state :deed))
                                contract-address)
    :owner))

