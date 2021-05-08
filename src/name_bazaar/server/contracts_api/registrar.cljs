(ns name-bazaar.server.contracts-api.registrar
  (:require
    [cljs.nodejs :as nodejs]
    [cljs-web3-next.utils :refer [to-wei]]
    [district.server.smart-contracts :refer [contract-call contract-send]]
    [district.server.web3 :refer [web3]]))

(def root-node "eth")
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))

(defn register! [{:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (contract-send :eth-registrar
                 :register
                 [(sha3 label hash)]
                 (merge {:gas 2000000
                         :value (to-wei @web3 0.01 :ether)}
                        opts)))

(defn transfer! [{:keys [:ens.record/label :ens.record/owner]} & [opts]]
  (contract-send :eth-registrar
                 :transfer-from
                 [(:from opts) owner (sha3 label)]
                 (merge {:gas 2000000}
                        opts)))

(defn reclaim! [{:keys [:ens.record/label :ens.record/owner]} & [opts]]
  (contract-send :eth-registrar
                 :reclaim
                 [(sha3 label) owner]
                 (merge {:gas 2000000}
                        opts)))

(defn registration-owner [{:keys [:ens.record/hash :ens.record/label]}]
  (contract-call :eth-registrar :owner-of [(sha3 label hash)]))

(defn ens []
  (contract-call :eth-registrar :ens))
