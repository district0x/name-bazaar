(ns server.name-bazaar.utils
  (:require
    [bignumber.core :as bn]
    [cljs.core.async :refer [<! go]]
    [cljs.test :refer-macros [async]]
    [cljs-time.coerce :refer [from-long]]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.evm :as web3-evm]
    [cljs-web3-next.helpers :as web3-helpers]
    [district.server.web3 :refer [web3]]
    [district.shared.async-helpers :refer [promise->]]
    [mount.core :as mount]
    [taoensso.timbre :as log]))

(def snapshot-id (atom ""))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))


(defn now []
  (promise-> (web3-eth/get-block-number @web3)
             #(web3-eth/get-block @web3 % false)
             (fn [block]
               (let [block (web3-helpers/js->cljkk block)]
                 (from-long (* (:timestamp block) 1000))))))


(defn get-balance [address]
  (promise-> (web3-eth/get-balance @web3 address)
             bn/number))


(defn before-test []
  (async done
    (go
      (-> (mount/with-args
            {:web3            {:url        "ws://127.0.0.1:8549"
                               :on-online  #(log/warn "Ethereum node went online")
                               :on-offline #(log/warn "Ethereum node went offline")}
             :smart-contracts {:contracts-build-path "./resources/public/contracts-build/"
                               :contracts-var        #'name-bazaar.shared.smart-contracts/smart-contracts
                               :auto-mining?         true}})
          (mount/only [#'district.server.web3
                       #'district.server.smart-contracts/smart-contracts])
          (mount/start))
      (let [id (<! (web3-evm/snapshot @web3))]
        (swap! snapshot-id (fn [_] id)))
      (done))))


(defn after-test []
  (async done
    (go
      ;; we'd like to use cljs-web3-next.evm/revert here, but it's broken now as:
      ;; 1. evm_revert doesn't accept 0 arguments even if it should:
      ;;    https://github.com/trufflesuite/ganache-cli/issues/672
      ;; 2. evm_revert is a non-standard method not explicitly supported in web3js
      ;; 3. there's a validator expecting by default any non-standard method to have 0 args
      ;; so we need to go more low level
      (js-invoke (aget @web3 "currentProvider")
                 "send"
                 (clj->js {:jsonrpc "2.0",
                           :method "evm_revert",
                           :params [@snapshot-id],
                           :id 1})
                 (fn [err _]
                   (if err (throw (js/Error. "Error evm_revert-ing to snapshot" err)))
                   (mount/stop)
                   (js/setTimeout #(done) 500))))))
