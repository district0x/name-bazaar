(ns name-bazaar.contracts-api.ens
  (:require [cljs-web3.async.eth :as web3-eth]
            [district0x.server.state :as state]
            [district0x.server.utils :as u]
            [district0x.server.effects :as effects]))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn set-subnode-owner! [server-state {:keys [:root-node :label :owner] :as opts}]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :ens)
                                 :set-subnode-owner
                                 (namehash root-node)
                                 (sha3 label)
                                 owner
                                 (merge {:gas 200000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn set-owner! [server-state {:keys [:node :owner :name] :as opts}]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :ens)
                                 :set-owner
                                 (u/ensure-namehash name node)
                                 owner
                                 (merge {:gas 200000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn owner [{:keys [:node :name]}]
  (web3-eth/contract-call (state/instance :ens) :owner (u/ensure-namehash name node)))

(defn on-transfer-once [server-state & [{:keys [:node :blockchain-filter-opts]
                                         :as opts
                                         :or {:blockchain-filter-opts "latest"}}]]
  (u/watch-event-once (state/instance server-state :ens)
                      :Transfer
                      (select-keys opts [:node])
                      blockchain-filter-opts))
