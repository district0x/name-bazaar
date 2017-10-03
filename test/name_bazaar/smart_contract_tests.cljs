(ns name-bazaar.smart-contract-tests
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

(defn balance [address]
  (web3-eth-async/get-balance
   (:web3 @*server-state*)
   address))

(swap! *server-state* assoc :log-contract-calls? false :log-errors? false)

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
             (is (= (state/my-address 0) (second (<! (auction-offering-factory/emergency-multisig ss)))))
             (is (= (contract-address :offering-registry) (second (<! (auction-offering-factory/offering-registry ss)))))
             (is (= (contract-address :offering-requests) (second (<! (auction-offering-factory/offering-requests ss)))))

             (is (= (contract-address :mock-registrar) (second (<! (buy-now-offering-factory/registrar ss)))))
             (is (= (state/my-address 0) (second (<! (buy-now-offering-factory/emergency-multisig ss)))))
             (is (= (contract-address :offering-registry) (second (<! (buy-now-offering-factory/offering-registry ss)))))
             (is (= (contract-address :offering-requests) (second (<! (buy-now-offering-factory/offering-requests ss)))))


             (done)))))

(defn offering-status-keys [resp]
  (select-keys resp [:offering/address
                     :offering/offering-registry
                     :offering/type
                     :offering/top-level-name?
                     :offering/name
                     :offering/contains-special-char?
                     :offering/label-length
                     :offering/name-level
                     :offering/node
                     :offering/auction?
                     :offering/contains-non-ascii?
                     :offering/label-hash
                     :offering/original-owner
                     :offering/version
                     :offering/price
                     :offering/label
                     :offering/buy-now?
                     :offering/emergency-multisig
                     :offering/contains-number?
                     :offering/registrar
                     :offering/new-owner
                     ]))

(deftest create-buy-now-offering
  (async done
         (let [ss @*server-state*]
           (go
             (testing "Registering name"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "abc"}
                                                      {:from (state/my-address 0)})))))

             (testing "Making an instant offer"
               (is (tx-sent? (<! (buy-now-offering-factory/create-offering! ss
                                                                            {:offering/name "abc.eth"
                                                                             :offering/price (eth->wei 0.1)}
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
                 (testing "Offering parameters are correct"
                   (is (= (offering-status-keys
                           {:offering/address offering
                            :offering/offering-registry (contract-address :offering-registry)
                            :offering/type :buy-now-offering
                            :offering/top-level-name? true
                            :offering/name "abc.eth"
                            :offering/contains-special-char? false
                            :offering/label-length 3
                            :offering/name-level 1
                            :offering/node (namehash "abc.eth")
                            :offering/auction? false
                            :offering/contains-non-ascii? false
                            :offering/label-hash (sha3 "abc")
                            :offering/original-owner (state/my-address 0)
                            :offering/version 1
                            :offering/price 100000000000000000,
                            :offering/label "abc"
                            :offering/buy-now? true
                            :offering/emergency-multisig (state/active-address ss)
                            :offering/contains-number? false
                            :offering/registrar (contract-address :mock-registrar)
                            :offering/new-owner nil})
                          (offering-status-keys (last (<! (offering/get-offering ss
                                                                        offering)))))))
                 (testing "Can't buy TLD if offering owns no deed"
                   (is (tx-failed?
                        (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.1)
                                                                                    :from (state/my-address 1)})))))

                 (testing "Transferrnig ownership to the offering"
                   (is (tx-sent? (<! (registrar/transfer! ss
                                                          {:ens.record/label "abc" :ens.record/owner offering}
                                                          {:from (state/my-address 0)})))))

                 (testing "Making sure an offering isn't too greedy"
                   (is (tx-failed?
                        (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.10001)
                                                                                    :from (state/my-address 1)})))))
                 (testing "Making sure an offering isn't too generous too"
                   (is (tx-failed?
                        (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.09999)
                                                                                    :from (state/my-address 1)})))))
                 (testing "Offering accepts the exact value"
                   (is (tx-sent?
                        (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.1)
                                                                                    :from (state/my-address 1)})))))
                 (testing "Can't sell the offering twice"
                   (is (tx-failed?
                        (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.1)
                                                                                    :from (state/my-address 1)})))))

                 (testing "The name ownership must be transferred to the new owner"
                   (is (= (state/my-address 1) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                          "abc.eth")}))))))
                 (testing "Ensuring the new owner gets his deed"
                   (is (= (state/my-address 1) (last (<! (registrar/entry-deed-owner
                                                          ss {:ens.record/label "abc"}))))))
                 (testing "New-owner of the offering is set"
                   (is (=  (state/my-address 1) (:offering/new-owner (last (<! (offering/get-offering ss
                                                                                    offering)))))))))
             (done)))))

(deftest create-auction-offering
  (async done
         (let [ss @*server-state*]
           (go
             (testing "Registering name"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "abc"}
                                                      {:from (state/my-address 0)})))))
             (testing "Offering the name with overdue endtime fails"
               (is (tx-failed? (<! (auction-offering-factory/create-offering!
                                  ss
                                  {:offering/name "abc.eth"
                                   :offering/price (eth->wei 0.1)
                                   :auction-offering/end-time (to-epoch (time/minus (time/now) (time/seconds 1)))
                                   :auction-offering/extension-duration 0
                                   :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                  {:from (state/my-address 0)})))))
             (testing "Offering the name with 0 bidincrease fails"
               (is (tx-failed? (<! (auction-offering-factory/create-offering!
                                    ss
                                    {:offering/name "abc.eth"
                                     :offering/price (eth->wei 0.1)
                                     :auction-offering/end-time (to-epoch (time/plus (time/now) (time/weeks 1)))
                                     :auction-offering/extension-duration 0
                                     :auction-offering/min-bid-increase (web3/to-wei 0 :ether)}
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

                 (testing "Transferrnig ownership to the offer"
                   (is (tx-sent? (<! (registrar/transfer! ss
                                                          {:ens.record/label "abc" :ens.record/owner offering}
                                                          {:from (state/my-address 0)})))))
                 (testing "Can't bid below the price"
                   (is (tx-failed? (<! (auction-offering/bid! ss
                                                              {:offering/address offering}
                                                              {:value (web3/to-wei 0.09 :ether)
                                                               :from (state/my-address 1)})))))
                 (testing "Can place a proper bid"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.1 :ether)
                                                             :from (state/my-address 1)})))))
                 (testing "Next bid should respect min-bid-increase"
                   (is (tx-failed? (<! (auction-offering/bid! ss
                                                              {:offering/address offering}
                                                              {:value (web3/to-wei 0.11 :ether)
                                                               :from (state/my-address 2)})))))
                 (testing "Correct increase of the bid is accepted"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.2 :ether)
                                                             :from (state/my-address 2)})))))
                 (testing "Arbitrary increase of the bid is ok"
                   (is (tx-sent? (<! (auction-offering/bid! ss
                                                            {:offering/address offering}
                                                            {:value (web3/to-wei 0.33 :ether)
                                                             :from (state/my-address 3)})))))
                 (testing "State of the auction offering is correct"
                   (is (= {:auction-offering/min-bid-increase 100000000000000000
                           :auction-offering/winning-bidder (state/my-address 3)
                           :auction-offering/bid-count 3}
                          (select-keys (last (<! (auction-offering/get-auction-offering ss
                                                                                        offering)))

                                       [:auction-offering/min-bid-increase
                                        :auction-offering/winning-bidder
                                        :auction-offering/bid-count]))))

                 (testing "Can't finalize the auction prematurely"
                   (is (tx-failed? (<! (auction-offering/finalize! ss
                                                                   {:offering/address offering
                                                                    :offering/transferPrice true}
                                                                   {:from (state/my-address 0)})))))
                 (web3-evm/increase-time!
                  (state/web3 ss)
                  [(time/in-seconds (time/days 15))]
                  (fn nearfuture [_]
                    (go
                      (testing "User who was overbid, can successfully withdraw funds from auction offering."
                        (is (tx-sent? (<! (auction-offering/withdraw! ss
                                                                      {:address (state/my-address 1)
                                                                       :offering offering}
                                                                      {:from (state/my-address 1)})))))
                      (testing "Finalizing works when it's time"
                        (is (tx-sent? (<! (auction-offering/finalize! ss
                                                                      {:offering/address offering
                                                                       :offering/transferPrice true}
                                                                      {:from (state/my-address 0)})))))
                      (testing "Ensuring the new owner gets his name"
                        (is (= (state/my-address 3) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                               "abc.eth")}))))))
                      (testing "Ensuring the new owner gets his deed"
                        (is (= (state/my-address 3) (last (<! (registrar/entry-deed-owner
                                             ss {:ens.record/label "abc"}))))))

                      (let [balance-of-2 (last (<! (balance (state/my-address 2))))]
                        (testing "User who was overbid, can successfully withdraw funds from auction offering."
                          (is (tx-sent? (<! (auction-offering/withdraw! ss
                                                                        {:address (state/my-address 2)
                                                                         :offering offering}
                                                                        {:from (state/my-address 2)}))))
                          (is (< (- (.plus balance-of-2 (web3/to-wei 0.2 :ether))
                                    (last (<! (balance (state/my-address 2)))))
                                 100000))
                          ))
                      (done))))))))))

(deftest offering-tld-ownership
  (async done
         (let [ss @*server-state*]
           (go
             (testing "Registering name to transfer onwnership"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "notowned"}
                                                      {:from (state/my-address 0)}))))

               (let [deed-addr
                     (:registrar.entry.deed/address
                      (second
                       (<! (registrar/entry ss
                                            {:ens.record/label "notowned"}
                                            {:from (state/my-address 0)}))))]
                 (is (not (empty? deed-addr)))
                 (is (not (empty? (second (<! (deed/owner ss deed-addr (state/my-address 1)))))))))

             (testing "Registering name to transfer deed"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "notowndeed"}
                                                      {:from (state/my-address 0)}))))
               (is (tx-sent? (<! (registrar/transfer! ss
                                                      {:ens.record/label "notowndeed"
                                                       :ens.record/owner (state/my-address 0)}
                                                      {:from (state/my-address 0)})))))
             (testing
                 "Can't make an instant offer on not owned domain"
               (is (tx-failed? (<! (buy-now-offering-factory/create-offering! ss
                                                                            {:offering/name "notowned"
                                                                             :offering/price (eth->wei 0.1)}
                                                                            {:from (state/my-address 1)})))))
             (testing
                 "Can't make an instant offer on not owned domain"
               (is (tx-failed? (<! (buy-now-offering-factory/create-offering! ss
                                                                              {:offering/name "notowndeed"
                                                                               :offering/price (eth->wei 0.1)}
                                                                              {:from (state/my-address 1)})))))

             (testing
                 "Can't offer for bid name I don't manage"
                 (is (tx-failed? (<! (auction-offering-factory/create-offering!
                                    ss
                                    {:offering/name "notowned"
                                     :offering/price (eth->wei 0.1)
                                     :auction-offering/end-time (to-epoch (time/plus (time/now) (time/weeks 2)))
                                     :auction-offering/extension-duration 0
                                     :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                    {:from (state/my-address 0)})))))
             (testing
                   "Can't offer for bid name I don't own"
               (is (tx-failed? (<! (auction-offering-factory/create-offering!
                                    ss
                                    {:offering/name "notowndeed"
                                     :offering/price (eth->wei 0.1)
                                     :auction-offering/end-time (to-epoch (time/plus (time/now) (time/weeks 2)))
                                     :auction-offering/extension-duration 0
                                     :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                                    {:from (state/my-address 0)})))))
             (done)))))

(deftest offering-subdomain-ownership
  (async done
         (let [ss @*server-state*]
           (go
             (testing "Registering name to add subdomain"
               (is (tx-sent? (<! (registrar/register! ss
                                                      {:ens.record/label "tld"}
                                                      {:from (state/my-address 0)}))))
               (is (tx-sent? (<! (ens/set-subnode-owner!
                                  ss
                                  {:ens.record/label "mysub"
                                   :ens.record/node "tld.eth"
                                   :ens.record/owner (state/my-address 0)}
                                  {:from (state/my-address 0)}))))

               (is (= (state/my-address 0) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                       "mysub.tld.eth")})))))
               (is (tx-sent? (<! (ens/set-subnode-owner!
                                  ss
                                  {:ens.record/label "theirsub"
                                   :ens.record/node "tld.eth"
                                   :ens.record/owner (state/my-address 1)}
                                  {:from (state/my-address 0)}))))
               (is (= (state/my-address 0) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                      "tld.eth")})))))
               (is (= (state/my-address 1) (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                                      "theirsub.tld.eth")}))))))

             (testing
                 "Can't make an instant offer if only deed-owner"
               (is (tx-failed? (<! (buy-now-offering-factory/create-offering! ss
                                                                              {:offering/name "theirsub.tld.eth"
                                                                               :offering/price (eth->wei 0.1)}
                                                                              {:from (state/my-address 0)})))))
             (testing
                 "Making an instant offer as an administrator"
               (is (tx-sent? (<! (buy-now-offering-factory/create-offering! ss
                                                                              {:offering/name "theirsub.tld.eth"
                                                                               :offering/price (eth->wei 0.1)}
                                                                              {:from (state/my-address 1)})))))
             (let [[[_ {{:keys [:offering]} :args}]]
                   (alts! [(offering-registry/on-offering-added-once ss
                                                                     {:node
                                                                      (namehash
                                                                       "theirsub.tld.eth")
                                                                      :from-block 0
                                                                      :owner (state/my-address 1)})
                           (timeout 5000)])]
               (testing
                   "Can't buy it yet, as subdomain ownership not transferred"
                 (is (tx-failed?
                      (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.1)
                                                                                  :from (state/my-address 2)})))))
               (testing
                   "Transferrnig ownership to the offering"
                 (is
                  (tx-sent?
                   (<! (ens/set-owner!
                        ss
                        {:ens.record/node (namehash "theirsub.tld.eth")
                         :ens.record/owner offering}
                        {:from (state/my-address 1)})))))
               (testing
                   "Now it can be sold"
                 (is (tx-sent?
                      (<! (buy-now-offering/buy! ss {:offering/address offering} {:value (eth->wei 0.1)
                                                                                  :from (state/my-address 2)})))))

               (testing
                   "The new owner changes"
                 (is (= (state/my-address 2)
                        (last (<! (ens/owner ss {:ens.record/node (namehash
                                                                   "theirsub.tld.eth")})))))))

             (testing
                 "Making an instant offer if not an owner fails"
               (is (tx-failed? (<! (buy-now-offering-factory/create-offering! ss
                                                                              {:offering/name "mysub.tld.eth"
                                                                               :offering/price (eth->wei 0.1)}
                                                                              {:from (state/my-address 1)})))))

             (testing
                 "Making an instant offer as a owner"
               (is (tx-sent? (<! (buy-now-offering-factory/create-offering! ss
                                                                            {:offering/name "mysub.tld.eth"
                                                                             :offering/price (eth->wei 0.1)}
                                                                            {:from (state/my-address 0)})))))
             (done)))))
