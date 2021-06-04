(ns server.name-bazaar.smart-contract-ext-tests
  (:require
    [bignumber.core :as bn]
    [cljs.core.async :refer [<! go]]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.evm :as web3-evm]
    [cljs-web3-next.utils :refer [to-wei]]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [clojure.string :as string]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.auction-offering-factory :as auction-offering-factory]
    [name-bazaar.server.contracts-api.buy-now-offering :as buy-now-offering]
    [name-bazaar.server.contracts-api.buy-now-offering-factory :as buy-now-offering-factory]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.offering :as offering]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.registrar :as registrar]
    [name-bazaar.server.contracts-api.used-by-factories :as used-by-factories]
    [name-bazaar.shared.smart-contracts]
    [print.foo :include-macros true]
    [server.name-bazaar.utils :refer [after-test before-test get-balance namehash now sha3]]))

(use-fixtures
  :each {:before before-test
         :after after-test})

(deftest create-auction-offering
  (async done
    (go
      (let [[addr0 addr1 addr2] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr0}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time (to-epoch (t/plus (<! (now)) (t/weeks 2)))
                                      :auction-offering/extension-duration 0
                                      :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                     {:from addr0}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]

        (testing "Registering name"
          (is register-tx))

        (testing "Offering the name for a bid"
          (is create-offering-tx))

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (testing "Can place a proper bid"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr1}))]
            (is tx)))

        (testing "Correct increase of the bid is accepted"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.2 :ether)
                                               :from addr2}))]
            (is tx)))

        (testing "If user has bid before, he needs to send the whole amount again in order to make a new bid"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.3 :ether)
                                               :from addr1}))]
            (is tx)))

        (testing "State of the auction offering is correct"
          (is (= {:auction-offering/min-bid-increase 100000000000000000
                  :auction-offering/winning-bidder   (string/lower-case addr1)
                  :auction-offering/bid-count        3}
                 (select-keys (<! (auction-offering/get-auction-offering offering))
                              [:auction-offering/min-bid-increase
                               :auction-offering/winning-bidder
                               :auction-offering/bid-count])))))
      (done))))


(deftest create-subdomain-auction-offering
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            t0 (to-epoch (t/plus (<! (now)) (t/weeks 2)))
            register-tx (<! (registrar/register! {:ens.record/label "tld"}
                                                 {:from addr0}))
            set-subnode-owner-tx (<! (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                                              :ens.record/node "tld.eth"
                                                              :ens.record/owner addr1}
                                                             {:from addr0}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "theirsub.tld.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time t0
                                      :auction-offering/extension-duration (t/in-seconds (t/days 4))
                                      :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                     {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]
        (is register-tx)
        (is set-subnode-owner-tx)
        (is (= addr1 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))

        (testing "Offering the name for a bid"
          (is create-offering-tx))

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (testing "Can't place a bid before ownership transfer"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr1}))]
            (is (nil? tx))))

        (testing "Transferring ownership to the offer"
          (let [tx (<! (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                                :ens.record/node "tld.eth"
                                                :ens.record/owner offering}
                                               {:from addr0}))]
            (is tx)))

        (testing "Can place a proper bid"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr2}))]
            (is tx)))

        (<! (web3-evm/increase-time @web3 (t/in-seconds (t/days 13))))

        (let [balance-of-2 (<! (get-balance addr2))]
          (testing "Can place a bid"
            (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                {:value (to-wei @web3 0.3 :ether)
                                                 :from addr3}))]
              (is tx)))

          (testing "User who was overbid, should have his funds back from auction offering."
            (is (< (- (bn/+ balance-of-2 (bn/number (to-wei @web3 0.1 :ether)))
                      (<! (get-balance addr2)))
                   100000)))

          (testing "Nothing to withdraw if return transfer succeeded on overbid."
            (let [withdraw-tx (<! (auction-offering/withdraw! {:offering offering
                                                               :address addr2}
                                                              {:from addr2}))]
              (is withdraw-tx)
              (is (< (- (bn/+ balance-of-2 (bn/number (to-wei @web3 0.1 :ether)))
                        (<! (get-balance addr2)))
                     100000))))

          (testing "State of the auction offering is correct"
            (is (< (- (:auction-offering/end-time (<! (auction-offering/get-auction-offering offering)))
                      t0
                      (t/in-seconds (t/days 3)))
                   100))) ;; threshold on operations
          ))
      (done))))


(deftest subdomain-auction-withdraw
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            t0 (to-epoch (t/plus (<! (now)) (t/weeks 2)))
            register-tx (<! (registrar/register! {:ens.record/label "tld"}
                                                 {:from addr0}))
            set-subnode-owner-tx (<! (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                                              :ens.record/node "tld.eth"
                                                              :ens.record/owner addr1}
                                                             {:from addr0}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "theirsub.tld.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time t0
                                      :auction-offering/extension-duration (t/in-seconds (t/days 4))
                                      :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                     {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]
        (is register-tx)
        (is set-subnode-owner-tx)

        (testing "Offering the name for a bid"
          (is create-offering-tx))

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (testing "Transferring ownership to the offer"
          (let [tx (<! (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                                :ens.record/node "tld.eth"
                                                :ens.record/owner offering}
                                               {:from addr0}))]
            (is tx)))

        (testing "Can place a proper bid"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr2}))]
            (is tx)))

        (<! (web3-evm/increase-time @web3 (t/in-seconds (t/days 13))))

        (let [balance-of-2 (<! (get-balance addr2))]
          (testing "Can place a bid"
            (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                                {:value (to-wei @web3 0.3 :ether)
                                                 :from addr3}))]
              (is tx)))

          (testing "Emergency address can withdraw funds to a user's address on his behalf."
            (let [tx (<! (auction-offering/withdraw! {:offering offering
                                                      :address addr2}
                                                     {:from addr0}))]
              (is tx))

            (is (< (- (bn/+ balance-of-2 (bn/number (to-wei @web3 0.1 :ether)))
                      (<! (get-balance addr2)))
                   100000)))

          (testing "user can't withdraw twice."
            (let [tx (<! (auction-offering/withdraw! {:offering offering
                                                      :address addr2}
                                                     {:from addr2}))]
              (is tx))
            (is (< (- (bn/+ balance-of-2 (bn/number (to-wei @web3 0.1 :ether)))
                      (<! (get-balance addr2)))
                   100000)))

          (testing "State of the auction offering is correct"
            (is (< (- (:auction-offering/end-time (<! (auction-offering/get-auction-offering offering)))
                      t0
                      (t/in-seconds (t/days 3)))
                   100))) ;; threshold on operations
          ))
      (done))))


(deftest freezed-auction-offering-behaviour
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr1}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time (to-epoch (t/plus (<! (now)) (t/weeks 2)))
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

        (testing "Emergency multisig can pause the registry"
          (let [tx (<! (offering-registry/emergency-pause! {:from addr0}))]
            (is tx)))

        (testing "Can't place a bid, while the registry is paused "
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr2}))]
            (is (nil? tx))))

        (testing "Emergency multisig can release the registry"
          (let [tx (<! (offering-registry/emergency-release! {:from addr0}))]))

        (testing "Can place a bid, when it's resumed"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr2}))]
            (is tx)))

        (<! (web3-evm/increase-time @web3 (t/in-seconds (t/days 15))))

        (testing "Emergency multisig can pause the registry"
          (let [tx (<! (offering-registry/emergency-pause! {:from addr0}))]
            (is tx)))

        (testing "Finalizing is not possible on frozen registry"
          (let [tx (<! (auction-offering/finalize! offering {:from addr1}))]
            (is (nil? tx))))

        (testing "Emergency multisig can release the registry"
          (let [tx (<! (offering-registry/emergency-release! {:from addr0}))]
            (is tx)))

        (testing "Finalizing works when it's time"
          (let [tx (<! (auction-offering/finalize! offering {:from addr1}))]
            (is tx))))
      (done))))


(deftest freezed-buy-now-offering-behaviour
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr1}))
            create-offering-tx (<! (buy-now-offering-factory/create-offering! {:offering/name "abc.eth"
                                                                               :offering/price (to-wei @web3 0.1 :ether)}
                                                                              {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]
        (testing "Registering name"
          (is register-tx))

        (testing "Making an instant offer"
          (is create-offering-tx)

        (testing "Emergency multisig can pause the registry"
          (let [tx (<! (offering-registry/emergency-pause! {:from addr0}))]
            (is tx)))

        (testing "Offering can't be bought while registry is frozen"
          (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr2}))]
            (is (nil? tx))))

        (testing "Emergency multisig can release the registry"
          (let [tx (<! (offering-registry/emergency-release! {:from addr0}))]
            (is tx)))

        (testing "Offering accepts the exact value"
          (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr2}))]
            (is tx)))))
      (done))))
