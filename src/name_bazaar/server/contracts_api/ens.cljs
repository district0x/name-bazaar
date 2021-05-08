(ns name-bazaar.server.contracts-api.ens
  (:require
    [district.server.smart-contracts :refer [contract-call contract-send contract-event-in-tx]]))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn set-subnode-owner! [{:keys [:ens.record/node :ens.record/label :ens.record/owner]} & [opts]]
  (contract-send :ens
                 :set-subnode-owner
                 [(namehash node) (sha3 label) owner]
                 (merge {:gas 200000}
                        (select-keys opts [:from :to :gas-price :gas :value :data]))))

(defn set-owner! [{:keys [:ens.record/node :ens.record/owner :ens.record/name]} & [opts]]
  (contract-send :ens
                 :set-owner
                 [(if name (namehash name) node)
                  owner]
                 (merge {:gas 200000}
                        opts)))

(defn owner [{:keys [:ens.record/node :ens.record/name]}]
  (contract-call :ens :owner [(if name (namehash name) node)]))
