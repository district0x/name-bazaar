(ns name-bazaar.server.contracts-api.registrar
  (:require
    [cljs-web3.core :as web3]
    [cljs.nodejs :as nodejs]
    [district.server.smart-contracts :refer [contract-call]]
    [name-bazaar.server.contracts-api.ens :as ens]))

(def root-node "eth")
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))

(defn register! [{:keys [:ens.record/hash :ens.record/label]} & [opts]]
  (contract-call :name-bazaar-registrar
                 :register
                 (sha3 label hash)
                 (merge {:gas 2000000
                         :value (web3/to-wei 0.01 :ether)}
                        opts)))

(defn transfer! [{:keys [:ens.record/label :ens.record/owner]} & [opts]]
  {:set-owner-tx
     (ens/set-owner! {:ens.record/name (str label ".eth")
                      :ens.record/owner owner}
                     opts)
   :transfer-tx
     (contract-call :name-bazaar-registrar
                    :transferFrom
                    (:from opts)
                    owner
                    (sha3 label)
                    (merge {:gas 2000000}
                           opts))})

(defn registration-owner [{:keys [:ens.record/hash :ens.record/label]}]
  (contract-call :name-bazaar-registrar :owner-of (sha3 label hash)))

(defn ens []
  (contract-call :name-bazaar-registrar :ens))

(defn on-transfer [& args]
  (apply contract-call :name-bazaar-registrar :Transfer args))
