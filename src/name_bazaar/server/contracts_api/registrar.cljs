(ns name-bazaar.server.contracts-api.registrar
  (:require
    [cljs-web3.core :as web3]
    [cljs.nodejs :as nodejs]
    [district.server.smart-contracts :refer [contract-call]]
    [name-bazaar.server.contracts-api.deed :as deed]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]))

(def root-node "eth")
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))

(defn register! [{:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (contract-call :registrar
                 :register
                 (sha3 label hash)
                 (merge {:gas 2000000
                         :value (web3/to-wei 0.01 :ether)}
                        opts)))

(defn transfer! [{:keys [:ens.record/hash :ens.record/label :ens.record/owner]} & [opts]]
  (contract-call :registrar
                 :transfer
                 (sha3 label hash)
                 owner
                 (merge {:gas 2000000}
                        opts)))

(defn entry [{:keys [:ens.record/hash :ens.record/label]}]
  (parse-registrar-entry (contract-call :registrar :entries (sha3 label hash))))

(defn entry-deed-owner [args & [opts]]
  (deed/owner (:registrar.entry.deed/address (entry args))))

(defn ens []
  (contract-call :registrar :ens))

