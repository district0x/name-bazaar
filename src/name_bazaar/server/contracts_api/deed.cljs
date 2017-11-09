(ns name-bazaar.server.contracts-api.deed
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]))

(defn owner [contract-address & [opts]]
  (queue-contract-call! (web3-eth/contract-at (state/web3)
                                                    (:abi (state/contract :deed))
                                                    contract-address)
                        :owner))

