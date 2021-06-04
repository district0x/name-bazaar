(ns name-bazaar.server.contracts-api.buy-now-offering-factory
  (:require
    [district.server.smart-contracts :refer [contract-address contract-call contract-send]]
    [district.server.web3 :refer [web3]]
    [district0x.shared.utils :refer [abi-encode-params]]
    [name-bazaar.shared.utils :refer [name-label top-level-name?]]))

(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn create-offering! [{:keys [:offering/name :offering/price]} opts]
  (if (top-level-name? name)
    (contract-send :eth-registrar
                   :safe-transfer-from
                   [(:from opts)
                    (contract-address :buy-now-offering-factory)
                    (sha3 (name-label name))
                    (abi-encode-params @web3
                                       ["string" "uint"]
                                       [name price])]
                   (merge {:gas 450000} opts))
    (contract-send :buy-now-offering-factory
                   :create-subname-offering
                   [name price]
                   (merge {:gas 450000} opts))))

(defn ens []
  (contract-call :buy-now-offering-factory :ens))

(defn root-node []
  (contract-call :buy-now-offering-factory :root-node))

(defn offering-registry []
  (contract-call :buy-now-offering-factory :offering-registry))
