(ns name-bazaar.server.contracts-api.registrar
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]
    [name-bazaar.server.contracts-api.deed :as deed]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]
    [taoensso.timbre :as logging :refer-macros [info warn error]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def root-node "eth")

(defn register! [{:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (logged-contract-call! (state/instance :registrar)
                         :register
                         (d0x-server-utils/sha3 label hash)
                         (merge {:gas 2000000
                                 :from (state/active-address)}
                                opts)))

(defn transfer! [{:keys [:ens.record/hash :ens.record/label :ens.record/owner]} & [opts]]
  (logged-contract-call! (state/instance :registrar)
                         :transfer
                         (d0x-server-utils/sha3 label hash)
                         owner
                         (merge {:gas 2000000
                                 :from (state/active-address)}
                                opts)))

(defn entry [{:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (queue-contract-call!
    (chan 1 (map (fn [[err res]]
                   [err (parse-registrar-entry res)])))
    (state/instance :registrar) :entries (d0x-server-utils/sha3 label hash)))

(defn entry-deed-owner [args & [opts]]
  (let [ch (chan)]
    (go
      (let [[err entry] (<! (entry args))]
        (when err
          (logging/error "Error getting registrar entry" {:error err :args args} ::entry-deed-owner))
        (>! ch (<! (deed/owner (:registrar.entry.deed/address entry))))))
    ch))

(defn ens []
  (queue-contract-call! (state/instance :registrar) :ens))

