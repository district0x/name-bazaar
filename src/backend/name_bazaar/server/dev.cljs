(ns name-bazaar.server.dev
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.async.evm :as web3-evm-async]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state :refer [*server-state*]]
    [district0x.server.utils :as u]
    [district0x.server.utils :refer [watch-event-once]]
    [goog.date.Date]
    [name-bazaar.contracts-api.english-auction-offering :as english-auction-offering]
    [name-bazaar.contracts-api.english-auction-offering-factory :as english-auction-offering-factory]
    [name-bazaar.contracts-api.ens :as ens]
    [name-bazaar.contracts-api.instant-buy-offering :as instant-buy-offering]
    [name-bazaar.contracts-api.instant-buy-offering-factory :as instant-buy-offering-factory]
    [name-bazaar.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.contracts-api.used-by-factories :as used-by-factories]
    [name-bazaar.server.db :as db]
    [name-bazaar.server.db-generator :as db-generator]
    [honeysql.core :as sql]
    [honeysql.helpers :as sql-helpers]
    [name-bazaar.server.db-sync :as db-sync]
    [name-bazaar.server.effects :refer [deploy-smart-contracts!]]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [print.foo :include-macros true]
    )
  (:require-macros [cljs.core.async.macros :refer [go]]))

(nodejs/enable-util-print!)

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))
(def Web3 (js/require "web3"))
(set! js/Web3 Web3)

(def total-accounts 5)

(defn on-jsload []
  (println "on-jsload"))

(defn -main [& _]
  (d0x-effects/create-db! *server-state*)
  (d0x-effects/create-testrpc-web3! *server-state* {:total_accounts total-accounts})
  (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
  (go
    (<! (d0x-effects/load-my-addresses! *server-state*))))

(set! *main-cli-fn* -main)

(defn initialize! [server-state]
  (go
    (db-sync/stop-syncing!)
    (d0x-effects/load-smart-contracts! server-state smart-contracts)
    (<! (deploy-smart-contracts! server-state))
    (<! (db-generator/generate! @server-state {:total-accounts total-accounts}))
    (d0x-effects/create-db! server-state)
    (db-sync/start-syncing! @server-state)
    ))


(comment
  (.all (state/db) "SELECT * FROM offerings WHERE name LIKE '%.e%'" (comp println)))

(comment
  (web3-eth/get-block (state/web3) 1 println)
  (namehash "eth")
  (state/active-address)
  *server-state*
  (d0x-effects/create-testrpc-web3! *server-state* {:total_accounts total-accounts})
  (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
  (deploy-smart-contracts! *server-state*)
  (state/web3)
  (deploy-smart-contracts! *server-state*)
  (initialize! *server-state*)
  (state/instance :ens)
  (state/contract-address :ens)
  (web3-eth-async/set-default-account! (state/web3) "0x1")

  (do
    (d0x-effects/create-db! *server-state*)
    (db-sync/start-syncing! @*server-state*))

  (state/my-addresses)
  (go
    (print.foo/look (<! (web3-eth-async/accounts (state/web3)))))
  )