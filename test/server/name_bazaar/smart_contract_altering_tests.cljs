(ns server.name-bazaar.smart-contract-altering-tests
  (:require
    [bignumber.core :as bn]
    [cljs.core.async :refer [<! go]]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.evm :as web3-evm]
    [cljs-web3-next.utils :refer [to-wei]]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
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
    [name-bazaar.shared.smart-contracts]
    [print.foo :include-macros true]
    [server.name-bazaar.utils :refer [after-test before-test get-balance namehash now sha3 increase-time!]]))

(use-fixtures
  :each {:before before-test
         :after after-test})

(deftest offering-reclaiming-buy-now-tld
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
          (is create-offering-tx))

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (testing "Ensuring offering gets the registration"
          (is (= offering (<! (registrar/registration-owner {:ens.record/label "abc"})))))

        (testing "For Buy Now offering, original owner can reclaim name ownership in .eth registrar and ENS"
          (let [tx (<! (buy-now-offering/reclaim-ownership! offering {:from addr1}))]
            (is tx)))

        (testing "The name ownership must be transferred back to owner"
          (is (= addr1 (<! (ens/owner {:ens.record/node (namehash "abc.eth")})))))

        (testing "Ensuring the new owner gets back his registration"
          (is (= addr1 (<! (registrar/registration-owner {:ens.record/label "abc"}))))))
      (done))))


(deftest offering-reclaiming-buy-now-subdomain
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "tld"}
                                                 {:from addr1}))
            set-subnode-owner-tx (<! (ens/set-subnode-owner!
                                       {:ens.record/label "theirsub"
                                        :ens.record/node "tld.eth"
                                        :ens.record/owner addr2}
                                       {:from addr1}))
            create-offering-tx (<! (buy-now-offering-factory/create-offering!
                                     {:offering/name "theirsub.tld.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)}
                                     {:from addr2}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]
        (is register-tx)
        (is set-subnode-owner-tx)

        (testing "The name ownership must be transferred to the user"
          (is (= addr2 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))

        (testing "Making an instant offer"
            (is create-offering-tx)

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (testing "Transferring ownership to the offering"
          (let [tx (<! (ens/set-owner! {:ens.record/node (namehash "theirsub.tld.eth")
                                        :ens.record/owner offering}
                                       {:from addr2}))]
            (is tx)))

        (testing "The name ownership must be transferred to the offering"
          (is (= offering (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))

        (testing "For Buy Now offering, original owner can reclaim ENS name ownership"
          (let [tx (<! (buy-now-offering/reclaim-ownership! offering
                                                            {:from addr2}))]
            (is tx)))

        (testing "The name ownership must be transferred back to owner"
          (is (= addr2 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))))
      (done))))


(deftest offering-reclaiming-auction-tld
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

        (when offering
          (testing "Ensuring offering gets the registration"
            (is (= offering (<! (registrar/registration-owner {:ens.record/label "abc"})))))

          (testing "For Buy Now offering, original owner can reclaim name ownership in .eth registrar and ENS"
            (let [tx (<! (buy-now-offering/reclaim-ownership! offering {:from addr1}))]
              (is tx)))

          (testing "The name ownership must be transferred back to owner"
            (is (= addr1 (<! (ens/owner {:ens.record/node (namehash "abc.eth")})))))

          (testing "Ensuring the new owner gets back his registration"
            (is (= addr1 (<! (registrar/registration-owner {:ens.record/label "abc"})))))))
      (done))))


(deftest offering-reclaiming-auction-subdomain
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "tld"}
                                                 {:from addr1}))
            set-subnode-owner-tx (<! (ens/set-subnode-owner!
                                       {:ens.record/label "theirsub"
                                        :ens.record/node "tld.eth"
                                        :ens.record/owner addr2}
                                       {:from addr1}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "theirsub.tld.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time (to-epoch (t/plus (<! (now)) (t/weeks 2)))
                                      :auction-offering/extension-duration 0
                                      :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                     {:from addr2}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]

        (is register-tx)
        (is set-subnode-owner-tx)

        (testing "The name ownership must be transferred to the user"
          (is (= addr2 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))

        (testing "Offering the name for a bid"
          (is create-offering-tx))

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (when offering
          (testing "Transferring ownership to the offering"
            (let [tx (<! (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                                  :ens.record/node "tld.eth"
                                                  :ens.record/owner offering}
                                                 {:from addr1}))]
              (is tx)))

          (testing "The name ownership must be transferred to the offering"
            (is (= offering (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))

          (testing "For Buy Now offering, original owner can reclaim ENS name ownership"
            (let [tx (<! (buy-now-offering/reclaim-ownership! offering {:from addr2}))]
              (is tx)))

          (testing "The name ownership must be transferred back to owner"
            (is (= addr2 (<! (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))))
      (done))))


(deftest offering-reclaiming-auction-tld-emergency
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3 addr4] (<! (web3-eth/accounts @web3))
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

        (testing "Ensuring offering gets the registration"
          (is (= offering (<! (registrar/registration-owner {:ens.record/label "abc"})))))

        (testing "Can place a proper bid"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value (to-wei @web3 0.1 :ether)
                                               :from addr4}))]
            (is tx)))

        (let [balance-of-4 (<! (get-balance addr4))]
          (testing "For Buy Now offering, original owner can reclaim name ownership in .eth registrar and ENS"
            (let [tx (<! (buy-now-offering/reclaim-ownership! offering {:from addr0}))]
              (is tx)))

          (testing "The name ownership must be transferred back to owner"
            (is (= addr1 (<! (ens/owner {:ens.record/node (namehash "abc.eth")})))))

          (testing "Ensuring the new owner gets back his registration"
            (is (= addr1 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

          (testing "User who was overbid, can successfully withdraw funds from auction offering."
            (let [tx (<! (auction-offering/withdraw! {:offering offering
                                                      :address addr4}
                                                     {:from addr4}))]
              (is tx))
            (is (< (- (bn/+ balance-of-4 (bn/number (to-wei @web3 0.1 :ether)))
                      (<! (get-balance addr4)))
                   100000)))))
      (done))))


(deftest offering-editing-buy-now
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr1}))
            create-offering-tx (<! (buy-now-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)}
                                     {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]

        (testing "Registering name"
          (is register-tx))

        (testing "Making an instant offer"
          (is create-offering-tx)

        (testing "On-offering event should fire"
          (is (not (nil? offering))))

        (testing "Offering can be successfully edited by original owner, throws error if different address tries to edit"
          (let [tx (<! (buy-now-offering/set-settings! {:offering/address offering
                                                        :offering/price (to-wei @web3 0.2 :ether)}
                                                       {:from addr2}))]
            (is (nil? tx))))

        (testing "Updating price"
          (let [tx (<! (buy-now-offering/set-settings! {:offering/address offering
                                                        :offering/price (to-wei @web3 0.2 :ether)}
                                                       {:from addr1}))]
            (is tx)))

        (testing "The price is updated"
          (is (= (to-wei @web3 0.2 :ether) (str (:offering/price (<! (offering/get-offering offering)))))))))
      (done))))


(deftest offering-editing-auction
  (async done
    (go
      (let [[addr0 addr1 addr2 addr3 addr4] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr1}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time (to-epoch (t/plus (<!(now))(t/weeks 2)))
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

        (let [t0 (to-epoch (t/plus (<! (now)) (t/weeks 4)))]
          (testing "Auction offering can be edited"
            (let [tx (<! (auction-offering/set-settings!
                           {:offering/address offering
                            :offering/price (to-wei @web3 0.2 :ether)
                            :auction-offering/end-time t0
                            :auction-offering/extension-duration 10000
                            :auction-offering/min-bid-increase (to-wei @web3 0.2 :ether)}
                           {:from addr1}))]
              (is tx)))

          (testing "State of the auction offering is correct"
            (is (= {:auction-offering/end-time (js/Math.floor t0),
                    :auction-offering/extension-duration 10000,
                    :auction-offering/min-bid-increase 200000000000000000}
                   (select-keys (<! (auction-offering/get-auction-offering offering))
                                [:auction-offering/end-time
                                 :auction-offering/extension-duration
                                 :auction-offering/min-bid-increase])))))

        (testing "The price is updated"
          (is (= (to-wei @web3 0.2 :ether) (str (:offering/price (<! (offering/get-offering offering)))))))

        (testing "Can place a proper bid"
          (is (auction-offering/bid! {:offering/address offering}
                                     {:value (to-wei @web3 0.4 :ether)
                                      :from addr4})))

        (testing "Auction offering can be edited only when has 0 bids, throws error if otherwise."
          (let [tx (<! (auction-offering/set-settings!
                         {:offering/address offering
                          :offering/price (to-wei @web3 0.8 :ether)
                          :auction-offering/end-time (to-epoch (t/plus (<! (now)) (t/weeks 8)))
                          :auction-offering/extension-duration 20000
                          :auction-offering/min-bid-increase (to-wei @web3 0.3 :ether)}
                         {:from addr1}))]
            (is (nil? tx)))))
      (done))))

(deftest set-offering-fees-buy-now
  (async done
    (go
      (let [[addr0 addr1 addr2] (<! (web3-eth/accounts @web3))
            register-tx (<! (registrar/register! {:ens.record/label "abc"}
                                                 {:from addr1}))
            set-fees-tx (<! (offering-registry/change-offering-fees! 10000 {:from addr0}))
            price (to-wei @web3 0.1 :ether)
            create-offering-tx (<! (buy-now-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price price}
                                     {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))]
        (testing "Fees are set up by emergency multisig"
          (is set-fees-tx))

        (let [balance-of-multisig (<! (get-balance addr0))
              balance-of-1 (<! (get-balance addr1))]

          (testing "Offering accepts the correct value"
            (let [tx (<! (buy-now-offering/buy! {:offering/address offering}
                                                {:value price
                                                 :from addr2}))]
              (is tx)))

          (testing "Ensuring the new owner gets his registration"
            (is (= addr2 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

          (testing "Seller gets offering price minus fee and emergency multisig gets fee"
            (let [fee (/ (* price 10000) 1000000)]
              (is (= fee (- (<! (get-balance addr0)) balance-of-multisig)))
              (is (= (- price fee) (- (<! (get-balance addr1)) balance-of-1)))))))
      (done))))


(deftest set-offering-fees-auction
  (async done
    (go
      (let [[addr0 addr1 addr2] (<! (web3-eth/accounts @web3))
            set-fees-tx (<! (offering-registry/change-offering-fees! 1000 {:from addr0}))
            register-tx (<! (registrar/register! {:ens.record/label "abc"} {:from addr1}))
            create-offering-tx (<! (auction-offering-factory/create-offering!
                                     {:offering/name "abc.eth"
                                      :offering/price (to-wei @web3 0.1 :ether)
                                      :auction-offering/end-time (to-epoch (t/plus (<! (now)) (t/weeks 2)))
                                      :auction-offering/extension-duration 0
                                      :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                     {:from addr1}))
            {offering :offering} (<! (offering-registry/on-offering-added-in-tx create-offering-tx))
            bid-value (to-wei @web3 0.2 :ether)]

        (testing "Can place a proper bid"
          (let [tx (<! (auction-offering/bid! {:offering/address offering}
                                              {:value bid-value
                                               :from addr2}))]
            (is tx)))

        (<! (increase-time! (t/in-seconds (t/days 15))))

        (let [balance-of-multisig (<! (get-balance addr0))
              balance-of-1 (<! (get-balance addr1))]
          (testing "Finalizing works when it's time"
            (let [tx (<! (auction-offering/finalize! offering {:from addr2}))]
              (is tx)))

          (testing "Ensuring the new owner gets his registration"
            (is (= addr2 (<! (registrar/registration-owner {:ens.record/label "abc"})))))

          (testing "Seller gets offering price minus fee and emergency multisig gets fee"
            (let [fee (/ (* bid-value 1000) 1000000)]
              (is (= fee (- (<! (get-balance addr0)) balance-of-multisig)))
              (is (= (- bid-value fee) (- (<! (get-balance addr1)) balance-of-1)))))))
      (done))))