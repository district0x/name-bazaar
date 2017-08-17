(ns name-bazaar.smart-contract-tests
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as time]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth :refer [contract-call]]
    [cljs-web3.evm :as web3-evm]
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state :refer [*server-state* contract-address]]
    [district0x.server.utils :as d0x-server-utils :refer [tx-sent?]]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei wei->eth]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.auction-offering-factory :as auction-offering-factory]
    [name-bazaar.server.contracts-api.buy-now-offering :as buy-now-offering]
    [name-bazaar.server.contracts-api.buy-now-offering-factory :as buy-now-offering-factory]
    [name-bazaar.server.contracts-api.deed :as deed]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.contracts-api.mock-registrar :as registrar]
    [name-bazaar.server.contracts-api.used-by-factories :as used-by-factories]
    [name-bazaar.server.effects :refer [deploy-smart-contracts!]]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [print.foo :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def Web3 (js/require "web3"))
(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))
(set! js/Web3 Web3)

(def total-accounts 10)

(swap! *server-state* assoc :log-contract-calls? false)

(use-fixtures
  :each
  {:before
   (fn []
     (async done
       (go
         (let [web3 (d0x-effects/create-web3! *server-state*)]
           (<! (d0x-effects/start-testrpc! *server-state* {:total_accounts total-accounts :web3 web3}))
           (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
           (<! (d0x-effects/load-my-addresses! *server-state*))
           (<! (deploy-smart-contracts! *server-state*))
           (done)))))
   :after
   (fn []
     (async done (js/setTimeout #(done) 0)))
   })

(deftest contracts-setup
  (async done
    (go
      (let [ss @*server-state*]
        (is (= (contract-address :ens) (second (<! (registrar/ens ss)))))
        (is (= (contract-address :mock-registrar) (second (<! (auction-offering-factory/registrar ss)))))
        (is (= (contract-address :offering-registry) (second (<! (auction-offering-factory/offering-registry ss)))))
        (is (= (contract-address :offering-requests) (second (<! (auction-offering-factory/offering-requests ss)))))

        (is (= (contract-address :mock-registrar) (second (<! (buy-now-offering-factory/registrar ss)))))
        (is (= (contract-address :offering-registry) (second (<! (buy-now-offering-factory/offering-registry ss)))))
        (is (= (contract-address :offering-requests) (second (<! (buy-now-offering-factory/offering-requests ss)))))

        ;; TODO more

        (done)))))

(deftest create-offering
  (async done
    (go
      (let [ss @*server-state*]
        (is (tx-sent? (<! (registrar/register! ss {:ens.record/label "abc"} {:from (state/my-address 0)}))))
        (is (tx-sent? (<! (buy-now-offering-factory/create-offering! ss
                                                                     {:offering/name "abc.eth"
                                                                      :offering/price (eth->wei 0.1)}
                                                                     {:from (state/my-address 0)}))))

        ;; TODO more

        (done)))))



