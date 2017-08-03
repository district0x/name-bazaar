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
    [district0x.server.api-server :as rest-server]
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
    [print.foo :include-macros true]
    )
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
  (rest-server/start! port))

(defn -main [& _]
  (go
    (<! (d0x-effects/start-testrpc! *server-state* {:total_accounts total-accounts :port 8549}))
    (d0x-effects/create-db! *server-state*)
    (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
    (rest-server/start! port)
    (<! (d0x-effects/load-my-addresses! *server-state*))))

(set! *main-cli-fn* -main)

(defn initialize! [server-state-atom]
  (go
    (db-sync/stop-syncing!)
    (d0x-effects/load-smart-contracts! server-state-atom smart-contracts)
    (<! (deploy-smart-contracts! server-state-atom))
    (<! (db-generator/generate! @server-state-atom {:total-accounts total-accounts}))
    (d0x-effects/create-db! server-state-atom)
    (db-sync/start-syncing! @server-state-atom)
    ))


(comment)

(comment

  (go
    (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
    (<! (d0x-effects/deploy-smart-contract! *server-state* (merge {:from (state/active-address)
                                                                   :contract-key :test-one})))

    (<! (d0x-effects/deploy-smart-contract! *server-state* (merge {:from (state/active-address)
                                                                   :contract-key :test-two
                                                                   :args [(state/contract-address :test-one)]})))

    (d0x-effects/logged-contract-call! @*server-state*
                                       (state/instance :test-one)
                                       :buy
                                       {:gas 300000
                                        :from (state/active-address)
                                        :value 1})
    )

  (web3-evm-async/mine! (state/web3))
  (web3-eth/block-number (state/web3) println)

  (go
    (print.foo/look (<! (offering-requests/add-request! @*server-state*
                                                        {:offering-request/name "i.eth"}
                                                        {:form (state/my-address @*server-state* (rand-int total-accounts))}))))

  (go
    (print.foo/look (<! (offering-requests/add-request! @*server-state*
                                                        {:offering-request/name "obwuf.eth"}
                                                        {:form (state/my-address @*server-state* (rand-int total-accounts))}))))

  (go
    (let [node "0xd207027b2d1e45a6b8a0f8ee1770f9ef4a13dc7de33943418f5db61ce1505a06"]
      (print.foo/look (<! (web3-eth-async/contract-call (state/instance :offering-requests) :requests node)))
      (print.foo/look (<! (offering-requests/requests-counts @*server-state* {:offering-request/node node})))))

  (go
    (let [node "0x18e2e2dce965b0ce974fe3c754464f5fe075316ee8f0f91ab7907d0d16ba375c"]
      (print.foo/look (<! (web3-eth-async/contract-call (state/instance :offering-requests) :requests node)))
      (print.foo/look (<! (offering-requests/requests-counts @*server-state* {:offering-request/node node})))))

  (web3-eth/get-block (state/web3) 1 println)
  (namehash "eth")
  (state/active-address)
  *server-state*
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

  (go
    (print.foo/look (<! (db/offering-exists? (state/db) "0xc8a98f49833b2db209182acc764bd2515e335a82"))))

  (.run (state/db) "CREATE TABLE test (abc BOOLEAN NOT NULL DEFAULT FALSE)" println)
  (.run (state/db) "CREATE TABLE test1 (abc INTEGER DEFAULT NULL)" println)

  (.all (state/db) "SELECT * FROM test" println)
  (.all (state/db) "SELECT * FROM test1" println)
  (.run (state/db) "INSERT INTO test (abc) VALUES (?)" (clj->js true) println)
  (.run (state/db) "INSERT INTO test1 (abc) VALUES (NULL)" println)

  (.get (state/db) "SELECT 1 FROM offerings WHERE address = '0'" println)

  (go (print.foo/look (<! (db/search-offerings (state/db) {}))))
  (go (print.foo/look (<! (db/search-offerings (state/db) {:offering/original-owner "0x5d434a053b4cfc35a65aeb77126717228ca7dcbf"}))))
  (go (print.foo/look (<! (db/search-offerings (state/db) {:offering/max-price (web3/to-wei 2 :ether)}))))
  (go (print.foo/look (<! (db/search-offerings (state/db) {:offering/version :english-auction-offering
                                                           :order-by [[:price :asc]]}))))

  (go (print.foo/look (<! (db/search-offering-requests (state/db) {:order-by [[:requests-count :desc]]}))))

  (go (print.foo/look (<! (http/get "http://localhost:6200/offerings"
                                    {:query-params {:name "%.eth"
                                                    :node-owner? true
                                                    :order-by-columns [:price]
                                                    :order-by-dirs [:asc]}}))))

  (go (print.foo/look (<! (http/get "http://localhost:6200/offering-requests"
                                    {:query-params {:order-by-columns [:requests-count]
                                                    :order-by-dirs [:desc]}}))))

  (.all (state/db) "SELECT * FROM offering_requests" #(print.foo/look (js->clj %2)))

  )
