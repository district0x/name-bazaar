(ns server.name-bazaar.smart-contract-tests
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as time]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.nodejs :as nodejs]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei wei->eth]]
    [mount.core :as mount]
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
    [name-bazaar.server.deployer]
    [name-bazaar.shared.smart-contracts]
    [print.foo :include-macros true]))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

;; ganache-cli default value
(def gas-price 2e10)

(defn now []
  (from-long (* (:timestamp (web3-eth/get-block @web3 (web3-eth/block-number @web3))) 1000)))

(defn get-transaction-cost
  "Get the total transaction cost for the given `transaction-address`.

  Returns a BigNumber of the total wei that was required to carry out
  the transaction."
  [transaction-address]
  (let [transaction-receipt (web3-eth/get-transaction-receipt @web3 transaction-address)
        unit-of-gas-used (:gas-used transaction-receipt)]
    (bn/* unit-of-gas-used gas-price)))

(use-fixtures
  :each
  {:before
   (fn []
     (-> (mount/with-args
           {:web3 {:port 8549}
            :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts
                              :auto-mining? true}})
         (mount/only [#'district.server.web3
                      #'district.server.smart-contracts/smart-contracts
                      #'name-bazaar.server.deployer/deployer])
         (mount/start)))
   :after
   (fn []
     (mount/stop)
     (async done (js/setTimeout #(done) 500)))})


(deftest contracts-setup
  (is (= (contract-address :ens) (registrar/ens)))
  (is (= (contract-address :ens) (auction-offering-factory/ens)))
  (is (= (namehash "eth") (auction-offering-factory/root-node)))
  (is (= (contract-address :offering-registry) (auction-offering-factory/offering-registry)))
  (is (= (contract-address :offering-requests) (auction-offering-factory/offering-requests)))

  (is (= (contract-address :ens) (buy-now-offering-factory/ens)))
  (is (= (namehash "eth") (buy-now-offering-factory/root-node)))
  (is (= (contract-address :offering-registry) (buy-now-offering-factory/offering-registry)))
  (is (= (contract-address :offering-requests) (buy-now-offering-factory/offering-requests)))

  (is (= (first (web3-eth/accounts @web3)) (offering/emergency-multisig (contract-address :buy-now-offering))))
  (is (= (contract-address :ens) (offering/ens (contract-address :buy-now-offering))))
  (is (= (contract-address :offering-registry) (offering/offering-registry (contract-address :buy-now-offering))))

  (is (= (first (web3-eth/accounts @web3)) (offering/emergency-multisig (contract-address :auction-offering))))
  (is (= (contract-address :ens) (offering/ens (contract-address :auction-offering))))
  (is (= (contract-address :offering-registry) (offering/offering-registry (contract-address :auction-offering)))))


(defn offering-status-keys [resp]
  (select-keys resp [:offering/address
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
                     :offering/contains-number?
                     :offering/new-owner
                     :offering/supports-unregister?
                     :offering/unregistered?]))


(deftest create-buy-now-offering
  (let [[addr0 addr1] (web3-eth/accounts @web3)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr0})))

    (testing "Making an instant offer"
      (let [tx-hash (buy-now-offering-factory/create-offering! {:offering/name "abc.eth"
                                                                :offering/price (eth->wei 0.1)}
                                                               {:from addr0})]
        (is tx-hash)
        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "abc.eth")
                                                                  :from-block 0
                                                                  :owner addr0})]
          (testing "on-offering event should fire"
            (is (not (nil? offering))))
          (when offering
            (testing "Offering parameters are correct"
              (is (= (offering-status-keys
                       {:offering/address offering
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
                        :offering/original-owner addr0
                        :offering/version 2
                        :offering/price 100000000000000000
                        :offering/label "abc"
                        :offering/buy-now? true
                        :offering/contains-number? false
                        :offering/new-owner nil
                        :offering/unregistered? false
                        :offering/supports-unregister? true})
                     (offering-status-keys (offering/get-offering offering)))))

            (testing "Can't buy TLD if offering owns no deed"
              (is (thrown? :default (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.1)
                                                                                         :from addr1}))))

            (testing "Transferrnig ownership to the offering"
              (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                       {:from addr0})))

            (testing "Making sure an offering isn't too greedy"
              (is (thrown? :default
                           (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.10001)
                                                                                :from addr1}))))
            (testing "Making sure an offering isn't too generous too"
              (is (thrown? :default (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.09999)
                                                                                         :from addr1}))))
            (testing "Offering accepts the exact value"
              (is (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.1)
                                                                       :from addr1})))
            (testing "Can't sell the offering twice"
              (is (thrown? :default (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.1)
                                                                                         :from addr1}))))

            (testing "The name ownership must be transferred to the new owner"
              (is (= addr1 (ens/owner {:ens.record/node (namehash "abc.eth")}))))
            (testing "Ensuring the new owner gets his deed"
              (is (= addr1 (registrar/entry-deed-owner {:ens.record/label "abc"}))))
            (testing "New-owner of the offering is set"
              (is (= addr1 (:offering/new-owner (offering/get-offering offering)))))))))))


(deftest create-auction-offering
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr0})))
    (testing "Offering the name with overdue endtime fails"
      (is (thrown? :default (auction-offering-factory/create-offering!
                             {:offering/name "abc.eth"
                              :offering/price (eth->wei 0.1)
                              :auction-offering/end-time (to-epoch (time/minus (now) (time/seconds 1)))
                              :auction-offering/extension-duration 0
                              :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                             {:from addr0}))))
    (testing "Offering the name with 0 bidincrease fails"
      (is (thrown? :default (auction-offering-factory/create-offering!
                             {:offering/name "abc.eth"
                              :offering/price (eth->wei 0.1)
                              :auction-offering/end-time (to-epoch (time/plus (now) (time/weeks 1)))
                              :auction-offering/extension-duration 0
                              :auction-offering/min-bid-increase (web3/to-wei 0 :ether)}
                             {:from addr0}))))
    (let [tx-hash (auction-offering-factory/create-offering!
                   {:offering/name "abc.eth"
                    :offering/price (eth->wei 0.1)
                    :auction-offering/end-time (to-epoch (time/plus (now) (time/weeks 2)))
                    :auction-offering/extension-duration 0
                    :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                   {:from addr0})]
      (testing "Offering the name for a bid"
        (is tx-hash))

      (let [{{:keys [:offering]} :args}
            (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "abc.eth")
                                                                :from-block 0
                                                                :owner addr0})
            bid-value-user-2 (web3/to-wei 0.2 :ether)]
        (testing "on-offering event should fire"
          (is (not (nil? offering))))
        (when offering

          (testing "Transferrnig ownership to the offer"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr0})))
          (testing "Can't bid below the price"
            (is (thrown? :default (auction-offering/bid! {:offering/address offering}
                                                         {:value (web3/to-wei 0.09 :ether)
                                                          :from addr1}))))
          (testing "Can place a proper bid"
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value (web3/to-wei 0.1 :ether)
                                        :from addr1})))
          (testing "Next bid should respect min-bid-increase"
            (is (thrown? :default (auction-offering/bid! {:offering/address offering}
                                                         {:value (web3/to-wei 0.11 :ether)
                                                          :from addr2}))))
          (testing "Correct increase of the bid is accepted"
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value bid-value-user-2
                                        :from addr2})))
          (let [balance-of-2 (web3-eth/get-balance @web3 addr2)]
            (testing "Arbitrary increase of the bid is ok"
              (is (auction-offering/bid! {:offering/address offering}
                                         {:value (web3/to-wei 0.33 :ether)
                                          :from addr3})))

            (testing "State of the auction offering is correct"
              (is (= {:auction-offering/min-bid-increase 100000000000000000
                      :auction-offering/winning-bidder addr3
                      :auction-offering/bid-count 3}
                     (select-keys (auction-offering/get-auction-offering offering)
                                  [:auction-offering/min-bid-increase
                                   :auction-offering/winning-bidder
                                   :auction-offering/bid-count]))))

            (testing "Can't finalize the auction prematurely"
              (is (thrown? :default (auction-offering/finalize! offering {:from addr0}))))

            (web3-evm/increase-time! @web3 [(time/in-seconds (time/days 15))])

            (testing "User who was overbid, can successfully withdraw funds from auction offering."
              (is (auction-offering/withdraw! {:address addr1
                                               :offering offering}
                                              {:from addr1})))
            (testing "Finalizing works when it's time"
              (is (auction-offering/finalize! offering {:from addr0})))
            (testing "Ensuring the new owner gets his name"
              (is (= addr3 (ens/owner {:ens.record/node (namehash "abc.eth")}))))
            (testing "Ensuring the new owner gets his deed"
              (is (= addr3 (registrar/entry-deed-owner {:ens.record/label "abc"}))))

            (testing "User who was overbid, getting his funds back from auction offering."
              ;; Note: transaction costs don't apply, `balance-of-2` is tracked after the bid
              (let [expected-balance (bn/+ balance-of-2 bid-value-user-2)
                    actual-balance (web3-eth/get-balance @web3 addr2)]
                (is (bn/zero? (bn/- expected-balance actual-balance)))))))))))


(deftest offering-tld-ownership
  (let [[addr0 addr1] (web3-eth/accounts @web3)]
    (testing "Registering name to transfer onwnership"
      (is (registrar/register! {:ens.record/label "notowned"}
                               {:from addr0}))

      (let [deed-addr (:registrar.entry.deed/address (registrar/entry {:ens.record/label "notowned"}))]
        (is (not (empty? deed-addr)))
        (is (not (empty? (deed/owner deed-addr))))))

    (testing "Registering name to transfer deed"
      (is (registrar/register! {:ens.record/label "notowndeed"}
                               {:from addr0}))
      (is (registrar/transfer! {:ens.record/label "notowndeed"
                                :ens.record/owner addr0}
                               {:from addr0})))

    (testing "Can't make an instant offer on not owned domain"
      (is (thrown? :default (buy-now-offering-factory/create-offering! {:offering/name "notowned"
                                                                        :offering/price (eth->wei 0.1)}
                                                                       {:from addr1}))))
    (testing "Can't make an instant offer on not owned domain"
      (is (thrown? :default (buy-now-offering-factory/create-offering! {:offering/name "notowndeed"
                                                                        :offering/price (eth->wei 0.1)}
                                                                       {:from addr1}))))

    (testing "Can't offer for bid name I don't manage"
      (is (thrown? :default (auction-offering-factory/create-offering!
                             {:offering/name "notowned"
                              :offering/price (eth->wei 0.1)
                              :auction-offering/end-time (to-epoch (time/plus (now) (time/weeks 2)))
                              :auction-offering/extension-duration 0
                              :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                             {:from addr0}))))
    (testing "Can't offer for bid name I don't own"
      (is (thrown? :default (auction-offering-factory/create-offering!
                             {:offering/name "notowndeed"
                              :offering/price (eth->wei 0.1)
                              :auction-offering/end-time (to-epoch (time/plus (now) (time/weeks 2)))
                              :auction-offering/extension-duration 0
                              :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                             {:from addr0}))))))

(deftest offering-subdomain-ownership
  (let [[addr0 addr1 addr2] (web3-eth/accounts @web3)]
    (testing "Registering name to add subdomain"
      (is (registrar/register! {:ens.record/label "tld"}
                               {:from addr0}))

      (is (ens/set-subnode-owner!
           {:ens.record/label "mysub"
            :ens.record/node "tld.eth"
            :ens.record/owner addr0}
           {:from addr0}))

      (is (= addr0 (ens/owner {:ens.record/node (namehash "mysub.tld.eth")})))
      (is (ens/set-subnode-owner!
           {:ens.record/label "theirsub"
            :ens.record/node "tld.eth"
            :ens.record/owner addr1}
           {:from addr0}))
      (is (= addr0 (ens/owner {:ens.record/node (namehash "tld.eth")})))
      (is (= addr1 (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))

    (testing "Can't make an instant offer if only deed-owner"
      (is (thrown? :default (buy-now-offering-factory/create-offering! {:offering/name "theirsub.tld.eth"
                                                                        :offering/price (eth->wei 0.1)}
                                                                       {:from addr0}))))
    (let [tx-hash (buy-now-offering-factory/create-offering! {:offering/name "theirsub.tld.eth"
                                                              :offering/price (eth->wei 0.1)}

                                                             {:from addr1})]
      (testing "Making an instant offer as an administrator"
        (is tx-hash))

      (let [{{:keys [:offering]} :args}
            (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "theirsub.tld.eth")
                                                                :from-block 0
                                                                :owner addr1})]
        (testing "Can't buy it yet, as subdomain ownership not transferred"
          (is (thrown? :default (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.1)
                                                                                     :from addr2}))))
        (testing "Transferrnig ownership to the offering"
          (is (ens/set-owner! {:ens.record/node (namehash "theirsub.tld.eth")
                               :ens.record/owner offering}
                              {:from addr1})))

        (testing "Now it can be sold"
          (is (buy-now-offering/buy! {:offering/address offering} {:value (eth->wei 0.1)
                                                                   :from addr2})))

        (testing "The new owner changes"
          (is (= addr2 (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))

      (testing "Making an instant offer if not an owner fails"
        (is (thrown? :default (buy-now-offering-factory/create-offering! {:offering/name "mysub.tld.eth"
                                                                          :offering/price (eth->wei 0.1)}
                                                                         {:from addr1}))))

      (testing "Making an instant offer as a owner"
        (is (buy-now-offering-factory/create-offering! {:offering/name "mysub.tld.eth"
                                                        :offering/price (eth->wei 0.1)}
                                                       {:from addr0}))))))

(deftest auction-offering-self-overbid
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr1})))

    (let [tx-hash (auction-offering-factory/create-offering!
                   {:offering/name "abc.eth"
                    :offering/price (eth->wei 0.1)
                    :auction-offering/end-time (to-epoch (time/plus (now) (time/weeks 2)))
                    :auction-offering/extension-duration 0
                    :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                   {:from addr1})]

      (testing "Offering the name for a bid"
        (is tx-hash))

      (let [{{:keys [:offering]} :args}
            (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "abc.eth")
                                                                :from-block 0
                                                                :owner addr1})]
        (testing "on-offering event should fire"
          (is (not (nil? offering))))

        (let [;; starting account balances
              balance-of-1 (web3-eth/get-balance @web3 addr1)
              balance-of-2 (web3-eth/get-balance @web3 addr2)
              balance-of-3 (web3-eth/get-balance @web3 addr3)
              
              ;; bid values
              bid-value-user-2 (web3/to-wei 0.1 :ether)
              bid-value-user-3 (web3/to-wei 0.2 :ether)
              bid-value-user-3-overbid (web3/to-wei 0.3 :ether)

              ;; A temporary transaction log store
              transaction-log (atom {})
              log-tx! (fn [id result] (swap! transaction-log assoc id result) result)]

          (testing "Transferring ownership to the offer"
            (is (log-tx! :t1-register
                         (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                              {:from addr1}))))

          (testing "User 2 can place a proper bid"
            (is (log-tx! :t2-user2-place-bid
                         (auction-offering/bid! {:offering/address offering}
                                                {:value bid-value-user-2
                                                 :from addr2}))))

          (testing "Ensure User 2 has made the appropriate bid transaction with gas costs."
            (let [bid-tx (:t2-user2-place-bid @transaction-log)
                  tx-total-cost (get-transaction-cost bid-tx)
                  expected-balance (bn/- balance-of-2 tx-total-cost)
                  expected-balance (bn/- expected-balance bid-value-user-2)
                  actual-balance (web3-eth/get-balance @web3 addr2)]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (testing "User 3 can place a proper bid too"
            (is (log-tx! :t3-user3-place-bid
                         (auction-offering/bid! {:offering/address offering}
                                                {:value bid-value-user-3
                                                 :from addr3}))))

          (testing "User 2, who was overbid, should have his funds back from auction offering."
            (let [;; The user will have his funds back, but he would still have paid a gas price
                  bid-tx (:t2-user2-place-bid @transaction-log)
                  tx-total-cost (get-transaction-cost bid-tx)
                  expected-balance (bn/- balance-of-2 tx-total-cost)
                  actual-balance (web3-eth/get-balance @web3 addr2)]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (testing "User 3 funds are spent on the bid"
            (let [bid-tx (:t3-user3-place-bid @transaction-log)
                  tx-total-cost (get-transaction-cost bid-tx)
                  expected-balance (bn/- balance-of-3 tx-total-cost)
                  expected-balance (bn/- expected-balance bid-value-user-3)
                  actual-balance (web3-eth/get-balance @web3 addr3)]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (testing "User 3 can overbid in order to afk himself"
            (is (log-tx! :t4-user3-place-overbid
                         (auction-offering/bid! {:offering/address offering}
                                                {:value bid-value-user-3-overbid
                                                 :from addr3}))))

          (testing "User 3 who overbid himself, gets back only his own previous bids."
            (let [bid-tx-3 (:t3-user3-place-bid @transaction-log)
                  bid-tx-4 (:t4-user3-place-overbid @transaction-log)
                  tx-total-cost (bn/+ (get-transaction-cost bid-tx-3)
                                      (get-transaction-cost bid-tx-4))
                  expected-balance (bn/- balance-of-3 tx-total-cost)
                  expected-balance (bn/- expected-balance bid-value-user-3-overbid)
                  actual-balance (web3-eth/get-balance @web3 addr3)]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (web3-evm/increase-time! @web3 [(time/in-seconds (time/days 15))])

          (testing "State of the auction offering is correct"
            (is (= {:auction-offering/min-bid-increase 100000000000000000
                    :auction-offering/winning-bidder addr3
                    :auction-offering/bid-count 3}
                   (select-keys (auction-offering/get-auction-offering offering)
                                [:auction-offering/min-bid-increase
                                 :auction-offering/winning-bidder
                                 :auction-offering/bid-count]))))

          (testing "Finalizing works when it's time"
            (is (log-tx! :t5-user1-finalize
                         (auction-offering/finalize! offering {:from addr0}))))
          (testing "Ensuring the new owner gets his name"
            (is (= addr3 (ens/owner {:ens.record/node (namehash "abc.eth")}))))
          (testing "Ensuring the new owner gets his deed"
            (is (= addr3 (registrar/entry-deed-owner {:ens.record/label "abc"}))))

          (testing "Ensuring the previous owner gets the funds"
            (let [bid-tx-1 (:t1-register @transaction-log)
                  tx-total-cost (get-transaction-cost bid-tx-1)
                  expected-balance (bn/+ balance-of-1 bid-value-user-3-overbid)
                  expected-balance (bn/- expected-balance tx-total-cost)
                  actual-balance (web3-eth/get-balance @web3 addr1)]
              (is (bn/zero? (bn/- expected-balance actual-balance))))))))))

(deftest create-auction-offering-sanity-checks
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr0})))
    (testing "Offering with the endtime too far in the future fails"
      (is (thrown? :default (auction-offering-factory/create-offering!
                             {:offering/name "abc.eth"
                              :offering/price (eth->wei 0.1)
                              :auction-offering/end-time (to-epoch (time/plus (now)
                                                                              (time/days (* 4 30))
                                                                              (time/hours 1)))
                              :auction-offering/extension-duration 0
                              :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                             {:from addr0}))))
    (testing "Offering with the extension duration longer than auction duration fails"
      (is (thrown? :default (auction-offering-factory/create-offering!
                             {:offering/name "abc.eth"
                              :offering/price (eth->wei 0.1)
                              :auction-offering/end-time (to-epoch (time/plus (now) (time/days (* 2 30))))
                              :auction-offering/extension-duration (time/in-seconds (time/days 61))
                              :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                             {:from addr0}))))))
