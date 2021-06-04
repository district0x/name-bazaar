(ns name-bazaar.server.contracts-api.auction-offering-factory
  (:require
    [district.server.smart-contracts :refer [contract-address contract-call contract-send]]
    [district.server.web3 :refer [web3]]
    [district0x.shared.utils :refer [abi-encode-params]]
    [name-bazaar.shared.utils :refer [name-label top-level-name?]]))

(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn create-offering! [args opts]
  (let [args-order [:offering/name
                    :offering/price
                    :auction-offering/end-time
                    :auction-offering/extension-duration
                    :auction-offering/min-bid-increase]]
    (if (top-level-name? (:offering/name args))
      (contract-send :eth-registrar
                     :safe-transfer-from
                     [(:from opts)
                      (contract-address :auction-offering-factory)
                      (sha3 (name-label (:offering/name args)))
                      (abi-encode-params @web3
                                         ["string" "uint" "uint64" "uint64" "uint"]
                                         ((apply juxt args-order) args))]
                     (merge {:gas 500000}
                            opts))
      (contract-send :auction-offering-factory
                     :create-subname-offering
                     ((apply juxt args-order) args)
                     (merge {:gas 500000}
                            opts)))))

(defn ens []
  (contract-call :auction-offering-factory :ens))

(defn root-node []
  (contract-call :auction-offering-factory :root-node))

(defn offering-registry []
  (contract-call :auction-offering-factory :offering-registry))
