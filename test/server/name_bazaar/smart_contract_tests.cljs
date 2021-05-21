(ns server.name-bazaar.smart-contract-tests
  (:require
    [bignumber.core :as bn]
    [cljs.core.async :refer [<! go]]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as time]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.evm :as web3-evm]
    [cljs-web3-next.helpers :as web3-helpers]
    [cljs-web3-next.utils :refer [to-wei]]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [clojure.string :as string]
    [district.shared.async-helpers :refer [promise->]]
    [district.server.smart-contracts :refer [contract-address instance]]
    [district.server.web3 :refer [web3]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.auction-offering-factory :as auction-offering-factory]
    [name-bazaar.server.contracts-api.buy-now-offering :as buy-now-offering]
    [name-bazaar.server.contracts-api.buy-now-offering-factory :as buy-now-offering-factory]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.registrar :as registrar]
    [name-bazaar.shared.smart-contracts]
    [print.foo :include-macros true]
    [server.name-bazaar.utils :refer [after-test before-test get-balance namehash now sha3]]))

;; ganache-cli default value
(def gas-price 2e10)

(use-fixtures
  :each {:before before-test
         :after after-test})

(defn get-transaction-cost
  "Get the total transaction cost for the given `transaction-address`.

  Returns a BigNumber of the total wei that was required to carry out
  the transaction."
  [transaction-address]
  (promise-> (web3-eth/get-transaction-receipt @web3 transaction-address)
             (fn [receipt]
               (bn/* (:gas-used (web3-helpers/js->cljkk receipt)) gas-price))))


(deftest contracts-setup
  (async done
    (go
      (is (= (contract-address :ens) (<! (registrar/ens))))
      (is (= (contract-address :ens) (<! (auction-offering-factory/ens))))
      (is (= (namehash "eth") (<! (auction-offering-factory/root-node))))
      (is (= (contract-address :offering-registry) (<! (auction-offering-factory/offering-registry))))

      (is (= (contract-address :ens) (<! (buy-now-offering-factory/ens))))
      (is (= (namehash "eth") (<! (buy-now-offering-factory/root-node))))
      (is (= (contract-address :offering-registry) (<! (buy-now-offering-factory/offering-registry))))

      (is (= (first (<! (web3-eth/accounts @web3))) (<! (offering/emergency-multisig (contract-address :buy-now-offering)))))
      (is (= (contract-address :ens) (<! (offering/ens (contract-address :buy-now-offering)))))
      (is (= (contract-address :offering-registry) (<! (offering/offering-registry (contract-address :buy-now-offering)))))

      (is (= (first (<! (web3-eth/accounts @web3))) (<! (offering/emergency-multisig (contract-address :auction-offering)))))
      (is (= (contract-address :ens) (<! (offering/ens (contract-address :auction-offering)))))
      (is (= (contract-address :offering-registry) (<! (offering/offering-registry (contract-address :auction-offering)))))
      (done))))


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
  (async done
    (go
      (let [[addr0 addr1] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"} {:from addr0}))]
        (testing "Register name"
          (is register-tx))

        (testing "Ownership check"
          (is (= addr0 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

        (testing "Making an instant offer"
          (let [create-offering-tx (<! (buy-now-offering-factory/create-offering! {:offering/name "abc.eth"
                                                                                   :offering/price (to-wei @web3 0.1 :ether)}
                                                                                  {:from addr0}))
                {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]

            (testing "Create offering transaction successful"
              (is create-offering-tx))

            (testing "On-offering event should fire"
              (is (not (nil? offering))))

            (testing "Offering parameters are correct"
              (is (= (offering-status-keys
                       {:offering/address (string/lower-case offering)
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
                        :offering/original-owner (string/lower-case addr0)
                        :offering/version 2
                        :offering/price 100000000000000000
                        :offering/label "abc"
                        :offering/buy-now? true
                        :offering/contains-number? false
                        :offering/new-owner nil
                        :offering/unregistered? false
                        :offering/supports-unregister? true})
                     (offering-status-keys (<! (offering/get-offering offering))))))

            (testing "Can't buy TLD if offering owns no registration"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.1 :ether)
                                                   :from addr1}))]
                (is (nil? tx))))

            (testing "Transferring ownership to the offering"
              (let [tx (<! (registrar/transfer! {:ens.record/label "abc"
                                                 :ens.record/owner offering}
                                                {:from addr0}))]
                (is tx)))

            (testing "Making sure an offering isn't too greedy"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.10001 :ether)
                                                   :from addr1}))]
                (is (nil? tx))))

            (testing "Making sure an offering isn't too generous either"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.09999 :ether)
                                                   :from addr1}))]
                (is (nil? tx))))

            (testing "Offering accepts the exact value"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.1 :ether)
                                                  :from addr1}))]
                (is tx)))

            (testing "Can't sell the offering twice"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.1 :ether)
                                                   :from addr1}))]
                (is (nil? tx))))

            (testing "ENS ownership must be transferred to the new owner"
              (is (= addr1 (<! (ens/owner {:ens.record/node (namehash "abc.eth")})))))

            (testing ".eth registrar ownership must be transferred to the new owner"
              (is (= addr1 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

            (testing "New-owner of the offering is set"
              (is (= (string/lower-case addr1) (:offering/new-owner (<! (offering/get-offering offering)))))))))
        (done))))


;; TODO this test can't have more assertions or it crashes
;; https://ask.clojure.org/index.php/10546/analyzer-error-when-there-are-more-than-21-tests-in-cljs-test
(deftest create-auction-offering
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"} {:from addr0}))]

        (testing "Offering the name with overdue endtime fails"
          (let [tx (<! (auction-offering-factory/create-offering!
                         {:offering/name "abc.eth"
                          :offering/price (to-wei @web3 0.1 :ether)
                          :auction-offering/end-time (to-epoch (time/minus (<! (now)) (time/seconds 1)))
                          :auction-offering/extension-duration 0
                          :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                         {:from addr0}))]
            (is (nil? tx))))

        (testing "Offering the name with 0 bidincrease fails"
          (let [tx (<! (auction-offering-factory/create-offering!
                         {:offering/name "abc.eth"
                          :offering/price (to-wei @web3 0.1 :ether)
                          :auction-offering/end-time (to-epoch (time/plus (<! (now)) (time/weeks 1)))
                          :auction-offering/extension-duration 0
                          :auction-offering/min-bid-increase (to-wei @web3 0 :ether)}
                         {:from addr0}))]
            (is (nil? tx))))

        (let [create-offering-tx (<! (auction-offering-factory/create-offering!
                                       {:offering/name "abc.eth"
                                        :offering/price (to-wei @web3 0.1 :ether)
                                        :auction-offering/end-time (to-epoch (time/plus (<! (now)) (time/weeks 2)))
                                        :auction-offering/extension-duration 0
                                        :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                       {:from addr0}))
              {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))
              bid-value-user-2 (to-wei @web3 0.2 :ether)]

          (testing "Offering the name for a bid"
            (is create-offering-tx))

          (testing "On-offering event should fire"
            (is (not (nil? offering))))

          (testing "Transferring ownership to the offer"
            (let [tx (<! (registrar/transfer! {:ens.record/label "abc"
                                               :ens.record/owner offering}
                                              {:from addr0}))]
              (is tx)))

          (testing "Can't bid below the price"
            (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                {:value (to-wei @web3 0.09 :ether)
                                                 :from addr1}))]
              (is (nil? tx))))

          (testing "Can place a proper bid"
            (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                {:value (to-wei @web3 0.1 :ether)
                                                 :from addr1}))]
              (is tx)))

          (testing "Next bid should respect min-bid-increase"
            (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                {:value (to-wei @web3 0.11 :ether)
                                                 :from addr1}))]
              (is (nil? tx))))

          (testing "Correct increase of the bid is accepted"
            (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                {:value bid-value-user-2
                                                 :from addr2}))]
              (is tx)))

          (let [balance-of-2 (<! (get-balance addr2))]
            (testing "Arbitrary increase of the bid is ok"
              (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                  {:value (to-wei @web3 0.33 :ether)
                                                   :from  addr3}))]
                (is tx)))

            (testing "State of the auction offering is correct"
              (is (= {:auction-offering/min-bid-increase 100000000000000000
                      :auction-offering/winning-bidder   (string/lower-case addr3)
                      :auction-offering/bid-count        3}
                     (select-keys (<! (auction-offering/get-auction-offering offering))
                                  [:auction-offering/min-bid-increase
                                   :auction-offering/winning-bidder
                                   :auction-offering/bid-count]))))

            (testing "Can't finalize the auction prematurely"
              (let [tx (<! (auction-offering/finalize! offering {:from addr0}))]
                (is (nil? tx))))

            (<! (web3-evm/increase-time @web3 (time/in-seconds (time/days 15))))

            (testing "User who was overbid, can successfully withdraw funds from auction offering."
              (let [tx (<! (auction-offering/withdraw! {:address addr1
                                                        :offering offering}
                                                       {:from addr2}))]
                (is tx)))

            (testing "Finalizing works when it's time"
              (let [tx (<! (auction-offering/finalize! offering {:from addr0}))]
                (is tx)))

            (testing "Ensuring the new owner gets his name"
              (is (= addr3 (<! (ens/owner {:ens.record/node (namehash "abc.eth")})))))

            (testing "Ensuring the new owner gets his registration"
              (is (= addr3 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

            (let [expected-balance (bn/+ balance-of-2 (bn/number bid-value-user-2))
                  actual-balance (<! (get-balance addr2))]
              (testing "User who was overbid, getting his funds back from auction offering."
                ;; Note: transaction costs don't apply, `balance-of-2` is tracked after the bid
                (is (bn/zero? (bn/- expected-balance actual-balance))))))))
      (done))))


(deftest offering-tld-ownership
  (async done
    (go
      (let [[addr0 addr1] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "tld"}
                                                 {:from addr0}))]

        (testing "Register tld, then transfer registration ownership, but retain registry ownership"
          (is register-tx)
          (is (= addr0 (<! (registrar/registration-owner {:ens.record/label "tld"}))))
          (let [tx (<! (registrar/transfer! {:ens.record/label "tld"
                                             :ens.record/owner addr1}
                                            {:from addr0}))]
            (is tx))
          (is (= addr1 (<! (registrar/registration-owner {:ens.record/label "tld"}))))
          (is (= addr0 (<! (ens/owner {:ens.record/name "tld.eth"})))))

        (testing "Can't make an instant offer on not owned domain"
          (let [tx (<! (buy-now-offering-factory/create-offering! {:offering/name "tld"
                                                                   :offering/price (to-wei @web3 0.1 :ether)}
                                                                  {:from addr1}))]
            (is (nil? tx))))

        (testing "Can't make an instant offer on domain we own in registry, but not in registrar"
          (let [tx (<! (buy-now-offering-factory/create-offering! {:offering/name "tld"
                                                                   :offering/price (to-wei @web3 0.1 :ether)}
                                                                  {:from addr0}))]
            (is (nil? tx))))

        (testing "Can't make auction offer on not owned domain"
          (let [tx (<! (auction-offering-factory/create-offering!
                         {:offering/name "tld"
                          :offering/price (to-wei @web3 0.1 :ether)
                          :auction-offering/end-time (to-epoch (time/plus (<! (now)) (time/weeks 2)))
                          :auction-offering/extension-duration 0
                          :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                         {:from addr1}))]
            (is (nil? tx))))

        (testing "Can't make auction offer on domain we own in registry, but not in registrar"
          (let [tx (<! (auction-offering-factory/create-offering!
                         {:offering/name "tld"
                          :offering/price (to-wei @web3 0.1 :ether)
                          :auction-offering/end-time (to-epoch (time/plus (<! (now)) (time/weeks 2)))
                          :auction-offering/extension-duration 0
                          :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                         {:from addr0}))]
            (is (nil? tx)))))
      (done))))


(deftest offering-subdomain-ownership
  (async done
    (go
      (let [[addr0 addr1 addr2] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "tld"}
                                                 {:from addr0}))
            set-subnode-owner-tx-1 (<! (ens/set-subnode-owner!
                                         {:ens.record/label "mysub"
                                          :ens.record/node "tld.eth"
                                          :ens.record/owner addr0}
                                         {:from addr0}))
            set-subnode-owner-tx-2 (<! (ens/set-subnode-owner!
                                         {:ens.record/label "theirsub"
                                          :ens.record/node "tld.eth"
                                          :ens.record/owner addr1}
                                         {:from addr0}))]

        (testing "Registering name to add subdomain"
          (is register-tx)
          (is (= addr0 (<! (ens/owner {:ens.record/node (namehash "tld.eth")}))))
          (is set-subnode-owner-tx-1)
          (is (= addr0 (<! (ens/owner {:ens.record/node (namehash "mysub.tld.eth")}))))
          (is set-subnode-owner-tx-2)
          (is (= addr1 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))

        (testing "Can't make an instant offer if we own parent domain, but not domain itself"
          (let [tx (<! (buy-now-offering-factory/create-offering! {:offering/name "theirsub.tld.eth"
                                                                   :offering/price (to-wei @web3 0.1 :ether)}
                                                                  {:from addr0}))]
            (is (nil? tx))))

        (testing "Making an instant offer as an administrator"
          (let [create-offering-tx (<! (buy-now-offering-factory/create-offering!
                                         {:offering/name "theirsub.tld.eth"
                                          :offering/price (to-wei @web3 0.1 :ether)}
                                         {:from addr1}))
                {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]
            (is create-offering-tx)

            (testing "Can't buy it yet, as subdomain ownership not transferred"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.1 :ether)
                                                   :from addr2}))]
                (is (nil? tx))))

            (testing "Transferring ownership to the offering"
              (let [tx (<! (ens/set-owner! {:ens.record/name "theirsub.tld.eth"
                                            :ens.record/owner offering}
                                           {:from addr1}))]
                (is tx)))

            (testing "Now it can be sold"
              (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                  {:value (to-wei @web3 0.1 :ether)
                                                   :from addr2}))]
                (is tx)))

            (testing "The new owner changes"
              (is (= addr2 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))))

          (testing "Making an instant offer if not an owner fails"
            (let [tx (<! (buy-now-offering-factory/create-offering! {:offering/name "mysub.tld.eth"
                                                                     :offering/price (to-wei @web3 0.1 :ether)}
                                                                    {:from addr1}))]
              (is (nil? tx))))

          (testing "Making an instant offer as a owner"
            (let [tx (<! (buy-now-offering-factory/create-offering! {:offering/name "mysub.tld.eth"
                                                                     :offering/price (to-wei @web3 0.1 :ether)}
                                                                    {:from addr0}))]
              (is tx)))))
      (done))))


(deftest auction-offering-self-overbid
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr1}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time (to-epoch (time/plus (<! (now)) (time/weeks 2)))
                                      :auction-offering/extension-duration 0
                                      :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                     {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]

        (testing "Registering name"
          (is register-tx))

        (testing "Offering the name for a bid"
          (is create-offering-tx))

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (let [;; starting account balances
              balance-of-1 (<! (get-balance addr1))
              balance-of-2 (<! (get-balance addr2))
              balance-of-3 (<! (get-balance addr3))

              ;; bid values
              bid-value-user-2 (to-wei @web3 0.1 :ether)
              bid-value-user-3 (to-wei @web3 0.2 :ether)
              bid-value-user-3-overbid (to-wei @web3 0.3 :ether)

              ;; A temporary transaction log store
              transaction-log (atom {})
              log-tx! (fn [id {:keys [:transaction-hash]}]
                        (swap! transaction-log assoc id transaction-hash) transaction-hash)]

          (testing "Transferring ownership to the offer"
            (is (log-tx! :t1-transfer
                         (<! (registrar/transfer! {:ens.record/label "abc"
                                                   :ens.record/owner offering}
                                                  {:from addr1})))))

          (testing "User 2 can place a proper bid"
            (is (log-tx! :t2-user2-place-bid
                         (<! (auction-offering/bid! {:offering/address offering}
                                                    {:value bid-value-user-2
                                                     :from addr2})))))

          (testing "Ensure User 2 has made the appropriate bid transaction with gas costs."
            (let [bid-tx (:t2-user2-place-bid @transaction-log)
                  tx-total-cost (<! (get-transaction-cost bid-tx))
                  expected-balance (bn/- balance-of-2 tx-total-cost)
                  expected-balance (bn/- expected-balance bid-value-user-2)
                  actual-balance (<! (get-balance addr2))]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (testing "User 3 can place a proper bid too"
            (is (log-tx! :t3-user3-place-bid
                         (<! (auction-offering/bid! {:offering/address offering}
                                                    {:value bid-value-user-3
                                                     :from addr3})))))

          ;; FIXME: Compare to transaction receipt for original gas spent
          ;; Issue # 131
          (testing "User 2, who was overbid, should have his funds back from auction offering."
            (let [;; The user will have his funds back, but he would still have paid a gas price
                  bid-tx (:t2-user2-place-bid @transaction-log)
                  tx-total-cost (<! (get-transaction-cost bid-tx))
                  expected-balance (bn/- balance-of-2 tx-total-cost)
                  actual-balance(<! (get-balance addr2))]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (testing "User 3 funds are spent on the bid"
            (let [bid-tx (:t3-user3-place-bid @transaction-log)
                  tx-total-cost (<! (get-transaction-cost bid-tx))
                  expected-balance (bn/- balance-of-3 tx-total-cost)
                  expected-balance (bn/- expected-balance bid-value-user-3)
                  actual-balance (<! (get-balance addr3))]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (testing "User 3 can overbid in order to afk himself"
            (is (log-tx! :t4-user3-place-overbid
                         (<! (auction-offering/bid! {:offering/address offering}
                                                    {:value bid-value-user-3-overbid
                                                     :from addr3})))))

          ;; FIXME: Compare to transaction receipt for original gas spent
          ;; Issue # 131
          (testing "User 3 who overbid himself, gets back only his own previous bids."
            (let [bid-tx-3 (:t3-user3-place-bid @transaction-log)
                  bid-tx-4 (:t4-user3-place-overbid @transaction-log)
                  tx-total-cost (bn/+ (<! (get-transaction-cost bid-tx-3))
                                      (<! (get-transaction-cost bid-tx-4)))
                  expected-balance (bn/- balance-of-3 tx-total-cost)
                  expected-balance (bn/- expected-balance (bn/number bid-value-user-3-overbid))
                  actual-balance (<! (get-balance addr3))]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))

          (<! (web3-evm/increase-time @web3 (time/in-seconds (time/days 15))))

          (testing "State of the auction offering is correct"
            (is (= {:auction-offering/min-bid-increase 100000000000000000
                    :auction-offering/winning-bidder (string/lower-case addr3)
                    :auction-offering/bid-count 3}
                   (select-keys (<! (auction-offering/get-auction-offering offering))
                                [:auction-offering/min-bid-increase
                                 :auction-offering/winning-bidder
                                 :auction-offering/bid-count]))))

          (testing "Finalizing works when it's time"
            (is (log-tx! :t5-user1-finalize
                         (<! (auction-offering/finalize! offering {:from addr0})))))

          (testing "Ensuring the new owner gets his name"
            (is (= addr3 (<! (ens/owner {:ens.record/node (namehash "abc.eth")})))))

          (testing "Ensuring the new owner gets his registration"
            (is (= addr3 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

          (testing "Ensuring the previous owner gets the funds"
            (let [bid-tx-1 (:t1-transfer @transaction-log)
                  tx-total-cost (<! (get-transaction-cost bid-tx-1))
                  expected-balance (bn/+ balance-of-1 (bn/number bid-value-user-3-overbid))
                  expected-balance (bn/- expected-balance tx-total-cost)
                  actual-balance (<! (get-balance addr1))]
              (is (bn/zero? (bn/- expected-balance actual-balance)))))))
      (done))))


(deftest create-auction-offering-sanity-checks
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))]
        (testing "Registering name"
          (let [tx (<! (registrar/register! {:ens.record/label "abc"}
                                            {:from addr0}))]
            (is tx)))

        (testing "Offering with the endtime too far in the future fails"
          (let [tx (<! (auction-offering-factory/create-offering!
                         {:offering/name "abc.eth"
                          :offering/price (to-wei @web3 0.1 :ether)
                          :auction-offering/end-time (to-epoch (time/plus (<! (now))
                                                                          (time/days (* 4 30))
                                                                          (time/hours 1)))
                          :auction-offering/extension-duration 0
                          :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                         {:from addr0}))]
            (is (nil? tx))))

        (testing "Offering with the extension duration longer than auction duration fails"
          (let [tx (<! (auction-offering-factory/create-offering!
                         {:offering/name "abc.eth"
                          :offering/price (to-wei @web3 0.1 :ether)
                          :auction-offering/end-time (to-epoch (time/plus (<! (now)) (time/days (* 2 30))))
                          :auction-offering/extension-duration (time/in-seconds (time/days 61))
                          :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                         {:from addr0}))]
            (is (nil? tx)))))
      (done))))
