(ns name-bazaar.server.contracts-api.ens
  (:require [cljs-web3.async.eth :as web3-eth-async]
            [district0x.server.state :as state]
            [district0x.server.utils :as d0x-server-utils]
            [district0x.server.effects :as effects]
            [cljs-web3.eth :as web3-eth]))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn set-subnode-owner! [server-state {:keys [:ens.record/node :ens.record/label :ens.record/owner]} & [opts]]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :ens)
                                 :set-subnode-owner
                                 (namehash node)
                                 (sha3 label)
                                 owner
                                 (merge {:gas 200000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn set-owner! [server-state {:keys [:ens.record/node :ens.record/owner :ens.record/name]} & [opts]]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :ens)
                                 :set-owner
                                 (d0x-server-utils/ensure-namehash name node)
                                 owner
                                 (merge {:gas 200000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn owner [server-state {:keys [:node :name]}]
  (web3-eth-async/contract-call (state/instance server-state :ens) :owner (d0x-server-utils/ensure-namehash name node)))

(defn on-transfer-once [server-state & args]
  (apply d0x-server-utils/watch-event-once (state/instance server-state :ens) :Transfer args))

(defn on-transfer [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :ens) :Transfer args))
