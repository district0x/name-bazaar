(ns name-bazaar.smart-contract-ext-tests
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

(swap! *server-state* assoc :log-contract-calls? false :log-errors? false)

(defn balance [address]
  (web3-eth-async/get-balance (:web3 @*server-state*)
                              address))

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

(deftest create-auction-offering
  (async done
         (let [ss @*server-state*]
           (go
             (testing "Registering name"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "abc"}
                                                      {:from (state/my-address 0)})))))
             (testing "Offering the name for a bid"
               (is (tx-sent? (<! (auction-offering-factory/create-offering!
                                  ss
                                  {:offering/name "abc.eth"
                                   :offering/price (eth->wei 0.1)
                                   :auction-offering/end-time (to-epoch (time/plus (time/now) (time/weeks 2)))
                                   :auction-offering/extension-duration 0
                                   :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                  {:from (state/my-address 0)})))))
             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "abc.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 0)})
                           (timeout 5000)])]
               (testing "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing "Can't place a bid before ownership transfer"
                   (is (tx-failed? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.1 :ether)
                                                             :from (state/my-address 1)})))))
                 (testing "Transferrnig ownership to the offer"
                   (is (tx-sent? (<! (registrar/transfer! ss
                                                          {:ens.record/label "abc" :ens.record/owner offering}
                                                          {:from (state/my-address 0)})))))
                 (testing "Can place a proper bid"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.1 :ether)
                                                             :from (state/my-address 1)})))))
                 (testing "Correct increase of the bid is accepted"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.2 :ether)
                                                             :from (state/my-address 2)})))))
                 (testing "If user has bid before, it's enough for him to send only difference needed for next bid."
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.2 :ether)
                                                             :from (state/my-address 1)})))))
                 (testing "State of the auction offering is correct"
                   (is (= {:auction-offering/min-bid-increase 100000000000000000
                           :auction-offering/winning-bidder (state/my-address 1)
                           :auction-offering/bid-count 3}
                          (select-keys (last (<! (auction-offering/get-auction-offering ss
                                                                                        offering)))

                                       [:auction-offering/min-bid-increase
                                        :auction-offering/winning-bidder
                                        :auction-offering/bid-count]))))
                 (done)))))))

(deftest create-subdomain-auction-offering
  (async done
         (let [ss @*server-state*
               t0 (to-epoch (time/plus (time/now) (time/weeks 2)))]
           (go
             (is (tx-sent? (<! (registrar/register! ss
                                                    {:ens.record/label "tld"}
                                                    {:from (state/my-address 0)}))))
             (is (tx-sent? (<! (ens/set-subnode-owner!
                                ss
                                {:ens.record/label "theirsub"
                                 :ens.record/node "tld.eth"
                                 :ens.record/owner (state/my-address 1)}
                                {:from (state/my-address 0)}))))
             #_(is (= (state/my-address 1) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                    "theirsub.tld.eth")})))))
             (testing "Offering the name for a bid"
               (is (tx-sent? (<! (auction-offering-factory/create-offering!
                                  ss
                                  {:offering/name "theirsub.tld.eth"
                                   :offering/price (eth->wei 0.1)
                                   :auction-offering/end-time t0
                                   :auction-offering/extension-duration (time/in-seconds (time/days 4))
                                   :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                  {:from (state/my-address 1)})))))
             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "theirsub.tld.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 1)})
                           (timeout 5000)])]
               (testing "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing "Can't place a bid before ownership transfer"
                   (is (tx-failed? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.1 :ether)
                                                             :from (state/my-address 1)})))))

                 (testing "Transferrnig ownership to the offer"
                   (is (tx-sent? (<! (ens/set-subnode-owner!
                                      ss
                                      {:ens.record/label "theirsub"
                                       :ens.record/node "tld.eth"
                                       :ens.record/owner offering}
                                      {:from (state/my-address 0)})))))
                 (testing "Can place a proper bid"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.1 :ether)
                                                             :from (state/my-address 2)})))))

                 (web3-evm/increase-time!
                  (state/web3 ss)
                  [(time/in-seconds (time/days 13))]
                  (fn nearfuture [_]

                    (go
                      (let [balance-of-2 (last (<! (balance (state/my-address 2))))]
                        (testing "Can place a bid"
                          (is (tx-sent? (<! (auction-offering/bid! ss
                                                                   {:offering/address offering}
                                                                   {:value (web3/to-wei 0.3 :ether)
                                                                    :from (state/my-address 3)})))))

                        (testing "User who was overbid, can successfully withdraw funds from auction offering."
                          (is (tx-sent? (<! (auction-offering/withdraw! ss
                                                                        {:offering offering
                                                                         :address (state/my-address 2)}
                                                                        {:from (state/my-address 2)}))))
                          (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                                    (last (<! (balance (state/my-address 2)))))
                                 100000)))
                        (testing "user can't withdraw twice."
                          (is (tx-sent? (<! (auction-offering/withdraw! ss
                                                                        {:offering offering
                                                                         :address (state/my-address 2)}
                                                                        {:from (state/my-address 2)}))))
                          (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                                    (last (<! (balance (state/my-address 2)))))
                                 100000)))
                        (testing "State of the auction offering is correct"
                          (is (< (- (:auction-offering/end-time (last (<! (auction-offering/get-auction-offering ss
                                                                                                                 offering))))
                                    t0
                                    (time/in-seconds (time/days 3)))
                                 10))) ;;threashold on operations
                        (done)))))))))))

(deftest subdomain-auction-withdraw
  (async done
         (let [ss @*server-state*
               t0 (to-epoch (time/plus (time/now) (time/weeks 2)))]
           (go
             (is (tx-sent? (<! (registrar/register! ss
                                                    {:ens.record/label "tld"}
                                                    {:from (state/my-address 0)}))))
             (is (tx-sent? (<! (ens/set-subnode-owner!
                                ss
                                {:ens.record/label "theirsub"
                                 :ens.record/node "tld.eth"
                                 :ens.record/owner (state/my-address 1)}
                                {:from (state/my-address 0)}))))

             (testing "Offering the name for a bid"
               (is (tx-sent? (<! (auction-offering-factory/create-offering!
                                  ss
                                  {:offering/name "theirsub.tld.eth"
                                   :offering/price (eth->wei 0.1)
                                   :auction-offering/end-time t0
                                   :auction-offering/extension-duration (time/in-seconds (time/days 4))
                                   :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                  {:from (state/my-address 1)})))))

             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "theirsub.tld.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 1)})
                           (timeout 5000)])]
               (testing "on-offering event should fire"
                 (is (not (nil? offering))))
               (when offering
                 (testing "Transferrnig ownership to the offer"
                   (is (tx-sent? (<! (ens/set-subnode-owner!
                                      ss
                                      {:ens.record/label "theirsub"
                                       :ens.record/node "tld.eth"
                                       :ens.record/owner offering}
                                      {:from (state/my-address 0)})))))
                 (testing "Can place a proper bid"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.1 :ether)
                                                             :from (state/my-address 2)})))))

                 (web3-evm/increase-time!
                  (state/web3 ss)
                  [(time/in-seconds (time/days 13))]
                  (fn nearfuture [_]

                    (go
                      (let [balance-of-2 (last (<! (balance (state/my-address 2))))]
                        (testing "Can place a bid"
                          (is (tx-sent? (<! (auction-offering/bid! ss
                                                                   {:offering/address offering}
                                                                   {:value (web3/to-wei 0.3 :ether)
                                                                    :from (state/my-address 3)})))))

                        (testing "Emergency address can withdraw funds to a user's address on his behalf."
                          (is (tx-sent? (<! (auction-offering/withdraw! ss
                                                                        {:offering offering
                                                                         :address (state/my-address 2)}
                                                                        {:from (state/my-address 0)}))))
                          (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                                    (last (<! (balance (state/my-address 2)))))
                                 100000)))
                        (testing "user can't withdraw twice."
                          (is (tx-sent? (<! (auction-offering/withdraw! ss
                                                                        {:offering offering
                                                                         :address (state/my-address 2)}
                                                                        {:from (state/my-address 2)}))))
                          (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                                    (last (<! (balance (state/my-address 2)))))
                                 100000)))
                        (testing "State of the auction offering is correct"
                          (is (< (- (:auction-offering/end-time (last (<! (auction-offering/get-auction-offering ss
                                                                                                                 offering))))
                                    t0
                                    (time/in-seconds (time/days 3)))
                                 10))) ;;threashold on operations
                        (done)))))))))))
