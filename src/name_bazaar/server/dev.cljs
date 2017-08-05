(ns name-bazaar.server.dev
  (:require
    [cljs-http.client :as http]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.async.evm :as web3-evm-async]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [cljs.spec.alpha :as s]
    [district0x.server.api-server :as api-server]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state :refer [*server-state*]]
    [district0x.server.utils :as u :refer [watch-event-once]]
    [goog.date.Date]
    [honeysql.core :as sql]
    [honeysql.helpers :as sql-helpers]
    [name-bazaar.server.api]
    [name-bazaar.server.contracts-api.english-auction-offering :as english-auction-offering]
    [name-bazaar.server.contracts-api.english-auction-offering-factory :as english-auction-offering-factory]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.instant-buy-offering :as instant-buy-offering]
    [name-bazaar.server.contracts-api.instant-buy-offering-factory :as instant-buy-offering-factory]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.contracts-api.used-by-factories :as used-by-factories]
    [name-bazaar.server.db :as db]
    [name-bazaar.server.db-generator :as db-generator]
    [name-bazaar.server.db-sync :as db-sync]
    [name-bazaar.server.effects :refer [deploy-smart-contracts!]]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [print.foo :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(set! js/XMLHttpRequest (nodejs/require "xhr2"))

(nodejs/enable-util-print!)

(def namehash (aget (nodejs/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))
(def Web3 (nodejs/require "web3"))
(set! js/Web3 Web3)

(def total-accounts 5)
(def port 6200)

(defn on-jsload []
  (api-server/start! port))

(defn -main [& _]
  (go
    (<! (d0x-effects/start-testrpc! *server-state* {:total_accounts total-accounts :port 8549}))
    (d0x-effects/create-db! *server-state*)
    (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
    (api-server/start! port)
    (<! (d0x-effects/load-my-addresses! *server-state*))))

(set! *main-cli-fn* -main)

(defn initialize! [server-state-atom]
  (go
    (db-sync/stop-syncing!)
    (d0x-effects/load-smart-contracts! server-state-atom smart-contracts)
    (<! (deploy-smart-contracts! server-state-atom))
    (<! (db-generator/generate! @server-state-atom {:total-accounts total-accounts}))
    (d0x-effects/create-db! server-state-atom)
    (db-sync/start-syncing! @server-state-atom)))


