(ns name-bazaar.server.dev
  (:require
    [cljs-http.client :as http]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.async.evm :as web3-evm-async]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan take!]]
    [cljs.nodejs :as nodejs]
    [cljs.pprint]
    [cljs.spec.alpha :as s]
    [district0x.server.api-server :as api-server]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.logging :as d0x-logging]
    [district0x.server.state :as state :refer [*server-state*]]
    [district0x.server.utils :as u :refer [watch-event-once]]
    [goog.date.Date]
    [honeysql.core :as sql]
    [honeysql.helpers :as sql-helpers]
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
    [name-bazaar.server.core]
    [name-bazaar.server.db :as db]
    [name-bazaar.server.db-generator :as db-generator]
    [name-bazaar.server.db-sync :as db-sync]
    [name-bazaar.server.effects :refer [deploy-smart-contracts!]]
    [name-bazaar.server.emailer.listeners :as listeners]
    [name-bazaar.server.watchdog :as watchdog]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [taoensso.timbre :as logging]
    [print.foo :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(nodejs/enable-util-print!)

(def namehash (aget (nodejs/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))
(def Web3 (nodejs/require "web3"))
(set! js/Web3 Web3)

(def total-accounts 6)

(defn on-jsload []
  (d0x-effects/load-config! state/default-config)
  (d0x-logging/setup! (state/config :logging))
  (logging/info "Final loaded config" (state/config) ::on-jsload)
  (api-server/start! (state/config :api-port))
  (d0x-effects/create-web3! {:port (state/config :testrpc-port)}))

(defn deploy-to-mainnet! [& [port deploy-opts]]
  (go
    (d0x-effects/load-config! state/default-config)
    (d0x-effects/create-web3! {:port (or port (state/config :mainnet-port))})
    (d0x-effects/load-smart-contracts! smart-contracts)
    (<! (d0x-effects/load-my-addresses!))
    (<! (deploy-smart-contracts! (merge {:persist? true} deploy-opts)))))

(defn initialize! []
  (let [ch (chan)]
    (go
      (watchdog/stop-syncing!)
      (d0x-effects/load-smart-contracts! smart-contracts)
      (<! (deploy-smart-contracts! {:persist? true}))
      (<! (db-generator/generate! {:total-accounts total-accounts}))
      (watchdog/start-syncing!)
      (>! ch true))
    ch))

(defn -main [& _]
  (go
    (d0x-effects/load-config! state/default-config)
    (d0x-logging/setup! (state/config :logging))
    (let [testrpc-port (state/config :testrpc-port)]
      (<! (d0x-effects/start-testrpc! {:total_accounts total-accounts :port testrpc-port}))
      (d0x-effects/create-web3! {:port testrpc-port})
      (d0x-effects/load-smart-contracts! smart-contracts)
      (api-server/start! (state/config :api-port))
      (<! (d0x-effects/load-my-addresses!)))))

(set! *main-cli-fn* -main)

(comment
  (deploy-to-mainnet! 8545 {:from "0x2a2A57a98a07D3CA5a46A0e1d51dEFffBeF54E4F"
                            :emergency-multisig "0x52f3f521c5f573686a78912995e9dedc5aae9928"
                            :skip-ens-registrar? true
                            :gas-price (web3/to-wei 4 :gwei)})
  (run-mainnet!)
  (name-bazaar.server.core/-main)
  (state/my-addresses)
  (name-bazaar.server.watchdog/start-syncing!))
