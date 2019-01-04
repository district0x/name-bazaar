(ns name-bazaar.server.dev
  (:require
    [cljs-http.client :as http]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :as pprint]
    [cljs.spec.alpha :as s]
    [district.server.config :refer [config]]
    [district.server.db :refer [db]]
    [district.server.endpoints :as endpoints]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.logging]
    [district.server.smart-contracts]
    [district.server.web3 :refer [web3]]
    [district.server.web3-watcher]
    [goog.date.Date]
    [honeysql.core :as sql]
    [honeysql.format :as sql-format]
    [honeysql.helpers :as sql-helpers]
    [mount.core :as mount]
    [name-bazaar.server.api]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.auction-offering-factory :as auction-offering-factory]
    [name-bazaar.server.contracts-api.buy-now-offering :as buy-now-offering]
    [name-bazaar.server.contracts-api.buy-now-offering-factory :as buy-now-offering-factory]
    [name-bazaar.server.contracts-api.deed :as deed]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.contracts-api.registrar :as registrar]
    [name-bazaar.server.contracts-api.used-by-factories :as used-by-factories]
    [name-bazaar.server.db]
    [name-bazaar.server.deployer]
    [name-bazaar.server.emailer]
    [name-bazaar.server.generator :as generator]
    [name-bazaar.server.syncer]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [print.foo :include-macros true]
    [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(def namehash (aget (nodejs/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))

(defn on-jsload []
  (mount/stop #'district.server.endpoints/endpoints)
  (mount/start #'district.server.endpoints/endpoints))

(defn deploy-to-mainnet []
  (mount/stop #'district.server.web3/web3
              #'district.server.smart-contracts/smart-contracts)
  (mount/start-with-args (merge
                           (mount/args)
                           {:web3 {:port 8545}
                            :deployer {:write? true
                                       :from "0x2a2A57a98a07D3CA5a46A0e1d51dEFffBeF54E4F"
                                       :emergency-multisig "0x52f3f521c5f573686a78912995e9dedc5aae9928"
                                       :skip-ens-registrar? true
                                       :gas-price (web3/to-wei 4 :gwei)}})
                         #'district.server.web3/web3
                         #'district.server.smart-contracts/smart-contracts))

(defn redeploy []
  (mount/stop)
  (-> (mount/with-args
        (merge
          (mount/args)
          {:deployer {:write? true}}))
    (mount/start)
    pprint/pprint))


(defn generate-data
  "Generate dev data"
  []
  (let [opts (or (:generator @config)
                 {:total-accounts 1
                  :offerings-per-account 1})]
    (log/info "Generating data, please be patient..." ::generate-date)
    (generator/generate opts)))


(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :endpoints {:port 6200
                                        :middlewares [logging-middlewares]}
                            :web3 {:port 8549}
                            :emailer {:print-mode? true
                                      :private-key "25677d268904ea651f84e37cfd580696c5c793dcd9730c415bf03b96003c09e9ef8"}
                            :ui {:public-key "2564e15aaf9593acfdc633bd08f1fc5c089aa43972dd7e8a36d67825cd0154602da47d02f30e1f74e7e72c81ba5f0b3dd20d4d4f0cc6652a2e719a0e9d4c7f10943"
                                 :use-instant-registrar? true
                                 :reveal-period {:hours 48}}}}
         :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}
         :deployer {:write? true}})
    (mount/except [#'name-bazaar.server.deployer/deployer])
    (mount/start)
    pprint/pprint))
