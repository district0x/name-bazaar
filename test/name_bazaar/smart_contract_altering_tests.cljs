(ns name-bazaar.smart-contract-altering-tests
  (:require
   [cljs-web3.async.eth :as web3-eth-async]
   [cljs-time.coerce :refer [to-epoch]]
   [cljs-time.core :as time]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth :refer [contract-call]]
   [cljs-web3.evm :as web3-evm]
   [cljs-web3.async.evm :as web3-async-evm]
   [cljs.core.async :refer [<! >! chan timeout alts!]]
   [cljs.nodejs :as nodejs]
   [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
   [district0x.server.effects :as d0x-effects]
   [district0x.server.state :as state :refer [*server-state* contract-address]]
   [district0x.server.utils :as d0x-server-utils :refer [tx-sent? tx-failed?]]
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
     (async done (js/setTimeout #(done) 0)))})

(deftest offering-reclaiming-buy-now-tld
  (async done
         (let [ss @*server-state*]
           (go
             (testing
                 "Registering name"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "abc"}
                                                      {:from (state/my-address 1)})))))

             (testing
                 "Making an instant offer"
               (is (tx-sent? (<! (buy-now-offering-factory/create-offering! ss
                                                                            {:offering/name "abc.eth"
                                                                             :offering/price (eth->wei 0.1)}
                                                                            {:from (state/my-address 1)})))))

             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "abc.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 1)})
                           (timeout 5000)])]
               (testing
                   "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing
                     "Transferrnig ownership to the offering"
                   (is (tx-sent? (<! (registrar/transfer! ss
                                                          {:ens.record/label "abc" :ens.record/owner offering}
                                                          {:from (state/my-address 1)})))))

                 (testing
                     "The name ownership must be transferred to the offering"
                   (is (= offering (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                              "abc.eth")}))))))

                 (testing
                     "Ensuring offering gets the deed"
                   (is (= offering (last (<! (registrar/entry-deed-owner
                                              ss {:ens.record/label "abc"}))))))
                 (testing
                     "For Buy Now offering, original owner can reclaim ENS name ownership (for TLD also deed ownership)"
                   (is (tx-sent? (<! (buy-now-offering/reclaim-ownership! ss
                                                                          offering
                                                                          {:from (state/my-address 1)})))))

                 (testing
                     "The name ownership must be transferred back to owner"
                   (is (= (state/my-address 1) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                          "abc.eth")}))))))
                 (testing
                     "Ensuring the new owner gets back his deed"
                   (is (= (state/my-address 1) (last (<! (registrar/entry-deed-owner
                                                          ss {:ens.record/label "abc"}))))))))
             ;; TODO more
             (done)))))

(deftest offering-reclaiming-buy-now-subdomain
  (async done
         (let [ss @*server-state*]
           (go
             (is (tx-sent? (<! (registrar/register! ss
                                                    {:ens.record/label "tld"}
                                                    {:from (state/my-address 1)}))))
             (is (tx-sent? (<! (ens/set-subnode-owner!
                                ss
                                {:ens.record/label "theirsub"
                                 :ens.record/node "tld.eth"
                                 :ens.record/owner (state/my-address 2)}
                                {:from (state/my-address 1)}))))
             (testing
                 "The name ownership must be transferred to the user"
               (is (= (state/my-address 2) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                          "theirsub.tld.eth")}))))))
             (testing
                 "Making an instant offer"
               (is (tx-sent? (<! (buy-now-offering-factory/create-offering! ss
                                                                            {:offering/name "theirsub.tld.eth"
                                                                             :offering/price (eth->wei 0.1)}
                                                                            {:from (state/my-address 2)})))))

             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "theirsub.tld.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 2)})
                           (timeout 5000)])]
               (testing
                   "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing
                     "Transferrnig ownership to the offering"
                   (is
                    (tx-sent? (<! (ens/set-subnode-owner!
                                   ss
                                   {:ens.record/label "theirsub"
                                    :ens.record/node  "tld.eth"
                                    :ens.record/owner offering}
                                   {:from (state/my-address 1)})))))

                 (testing
                     "The name ownership must be transferred to the offering"
                   (is (= offering (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                              "theirsub.tld.eth")}))))))

                 (testing
                     "For Buy Now offering, original owner can reclaim ENS name ownership"
                   (is (tx-sent? (<! (buy-now-offering/reclaim-ownership! ss
                                                                          offering
                                                                          {:from (state/my-address 2)})))))

                 (testing
                     "The name ownership must be transferred back to owner"
                   (is (= (state/my-address 2) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                          "theirsub.tld.eth")}))))))))
             ;; TODO more
             (done)))))

(deftest offering-reclaiming-auction-tld
  (async done
         (let [ss @*server-state*]
           (go
             (testing
                 "Registering name"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "abc"}
                                                      {:from (state/my-address 1)})))))


             (testing
                 "Offering the name for a bid"
               (is (tx-sent? (<! (auction-offering-factory/create-offering!
                                  ss
                                  {:offering/name "abc.eth"
                                   :offering/price (eth->wei 0.1)
                                   :auction-offering/end-time (to-epoch (time/plus (time/now) (time/weeks 2)))
                                   :auction-offering/extension-duration 0
                                   :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                  {:from (state/my-address 1)})))))

             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "abc.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 1)})
                           (timeout 5000)])]
               (testing
                   "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing
                     "Transferrnig ownership to the offering"
                   (is (tx-sent? (<! (registrar/transfer! ss
                                                          {:ens.record/label "abc" :ens.record/owner offering}
                                                          {:from (state/my-address 1)})))))

                 (testing
                     "The name ownership must be transferred to the offering"
                   (is (= offering (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                              "abc.eth")}))))))

                 (testing
                     "Ensuring offering gets the deed"
                   (is (= offering (last (<! (registrar/entry-deed-owner
                                              ss {:ens.record/label "abc"}))))))
                 (testing
                     "For Buy Now offering, original owner can reclaim ENS name ownership (for TLD also deed ownership)"
                   (is (tx-sent? (<! (buy-now-offering/reclaim-ownership! ss
                                                                          offering
                                                                          {:from (state/my-address 1)})))))

                 (testing
                     "The name ownership must be transferred back to owner"
                   (is (= (state/my-address 1) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                          "abc.eth")}))))))
                 (testing
                     "Ensuring the new owner gets back his deed"
                   (is (= (state/my-address 1) (last (<! (registrar/entry-deed-owner
                                                          ss {:ens.record/label "abc"}))))))))
             ;; TODO more
             (done)))))

(deftest offering-reclaiming-auction-subdomain
  (async done
         (let [ss @*server-state*]
           (go
             (is (tx-sent? (<! (registrar/register! ss
                                                    {:ens.record/label "tld"}
                                                    {:from (state/my-address 1)}))))
             (is (tx-sent? (<! (ens/set-subnode-owner!
                                ss
                                {:ens.record/label "theirsub"
                                 :ens.record/node "tld.eth"
                                 :ens.record/owner (state/my-address 2)}
                                {:from (state/my-address 1)}))))
             (testing
                 "The name ownership must be transferred to the user"
               (is (= (state/my-address 2) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                          "theirsub.tld.eth")}))))))
             (testing
                 "Offering the name for a bid"
               (is (tx-sent? (<! (auction-offering-factory/create-offering!
                                  ss
                                  {:offering/name "theirsub.tld.eth"
                                   :offering/price (eth->wei 0.1)
                                   :auction-offering/end-time (to-epoch (time/plus (time/now) (time/weeks 2)))
                                   :auction-offering/extension-duration 0
                                   :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                  {:from (state/my-address 2)})))))

             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "theirsub.tld.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 2)})
                           (timeout 5000)])]
               (testing
                   "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing
                     "Transferrnig ownership to the offering"
                   (is
                    (tx-sent? (<! (ens/set-subnode-owner!
                                   ss
                                   {:ens.record/label "theirsub"
                                    :ens.record/node  "tld.eth"
                                    :ens.record/owner offering}
                                   {:from (state/my-address 1)})))))

                 (testing
                     "The name ownership must be transferred to the offering"
                   (is (= offering (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                              "theirsub.tld.eth")}))))))

                 (testing
                     "For Buy Now offering, original owner can reclaim ENS name ownership"
                   (is (tx-sent? (<! (buy-now-offering/reclaim-ownership! ss
                                                                          offering
                                                                          {:from (state/my-address 2)})))))

                 (testing
                     "The name ownership must be transferred back to owner"
                   (is (= (state/my-address 2) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                          "theirsub.tld.eth")}))))))))
             ;; TODO more
             (done)))))
