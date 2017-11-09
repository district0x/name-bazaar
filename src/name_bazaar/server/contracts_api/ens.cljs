(ns name-bazaar.server.contracts-api.ens
  (:require
    [cljs-web3.eth :as web3-eth]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn set-subnode-owner! [{:keys [:ens.record/node :ens.record/label :ens.record/owner]} & [opts]]
  (logged-contract-call! (state/instance :ens)
                         :set-subnode-owner
                         (namehash node)
                         (sha3 label)
                         owner
                         (merge {:gas 200000
                                 :from (state/active-address)}
                                (select-keys opts [:from :to :gas-price :gas :value :data]))))

(defn set-owner! [{:keys [:ens.record/node :ens.record/owner :ens.record/name]} & [opts]]
  (logged-contract-call! (state/instance :ens)
                         :set-owner
                         (d0x-server-utils/ensure-namehash name node)
                         owner
                         (merge {:gas 200000
                                 :from (state/active-address)}
                                opts)))

(defn owner [{:keys [:ens.record/node :ens.record/name]}]
  (queue-contract-call! (state/instance :ens) :owner (d0x-server-utils/ensure-namehash name node)))

(defn on-transfer-once [& args]
  (apply d0x-server-utils/watch-event-once (state/instance :ens) :Transfer args))

(defn on-transfer [& args]
  (apply web3-eth/contract-call (state/instance :ens) :Transfer args))

(defn on-new-owner [& args]
  (apply web3-eth/contract-call (state/instance :ens) :NewOwner args))

(defn on-new-owner-once [& args]
  (apply d0x-server-utils/watch-event-once (state/instance :ens) :NewOwner args))
