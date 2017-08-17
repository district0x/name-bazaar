(ns name-bazaar.server.contracts-api.mock-registrar
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]
    [name-bazaar.server.contracts-api.deed :as deed])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def root-node "eth")

(defn register! [server-state {:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :mock-registrar)
                                 :register
                                 (d0x-server-utils/sha3 label hash)
                                 (merge {:gas 2000000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn transfer! [server-state {:keys [:ens.record/hash :ens.record/label :ens.record/owner]} & [opts]]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :mock-registrar)
                                 :transfer
                                 (d0x-server-utils/sha3 label hash)
                                 owner
                                 (merge {:gas 2000000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn entry [server-state {:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (web3-eth-async/contract-call
    (chan 1 (map (fn [[err res]]
                   [err (parse-registrar-entry res)])))
    (state/instance :mock-registrar) :entries (d0x-server-utils/sha3 label hash)))

(defn entry-deed-owner [server-state args & [opts]]
  (let [ch (chan)]
    (go
      (let [[err entry] (<! (entry server-state args))]
        (>! ch (<! (deed/owner server-state (:registrar.entry/deed entry))))))
    ch))

(defn ens [server-state]
  (web3-eth-async/contract-call (state/instance :mock-registrar) :ens))

