(ns server.name-bazaar.smart-contract-ext-tests
  (:require
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.nodejs :as nodejs]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.smart-contracts.core :refer [contract-address]]
    [district.server.web3.core :refer [web3 first-address my-addresses balance]]
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

(defn now []
  (from-long (* (:timestamp (web3-eth/get-block @web3 (web3-eth/block-number @web3))) 1000)))

(use-fixtures
  :each
  {:before
   (fn []
     (-> (mount/with-args
           {:web3 {:port 8549}
            :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts
                              :auto-mining? true}})
       (mount/only [#'district.server.web3.core
                    #'district.server.smart-contracts.core/smart-contracts
                    #'name-bazaar.server.deployer/deployer])
       (mount/start)))
   :after
   (fn []
     (mount/stop)
     (async done (js/setTimeout #(done) 3000)))})

(deftest create-auction-offering
  (let [[addr0 addr1 addr2] (my-addresses)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr0})))

    (let [tx-hash (auction-offering-factory/create-offering!
                    {:offering/name "abc.eth"
                     :offering/price (eth->wei 0.1)
                     :auction-offering/end-time (to-epoch (t/plus (now) (t/weeks 2)))
                     :auction-offering/extension-duration 0
                     :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                    {:from addr0})]

      (testing "Offering the name for a bid"
        (is tx-hash))

      (let [{{:keys [:offering]} :args}
            (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "abc.eth")
                                                                :from-block 0
                                                                :owner addr0})]
        (testing "on-offering event should fire"
          (is (not (nil? offering))))

        (testing "Can't place a bid before ownership transfer"
          (is (thrown? :default (auction-offering/bid! {:offering/address offering}
                                                       {:value (web3/to-wei 0.1 :ether)
                                                        :from addr1}))))
        (testing "Transferrnig ownership to the offer"
          (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                   {:from addr0})))
        (testing "Can place a proper bid"
          (is (auction-offering/bid! {:offering/address offering}
                                     {:value (web3/to-wei 0.1 :ether)
                                      :from addr1})))
        (testing "Correct increase of the bid is accepted"
          (is (auction-offering/bid! {:offering/address offering}
                                     {:value (web3/to-wei 0.2 :ether)
                                      :from addr2})))
        (testing "If user has bid before, he needs to send the whole amount again in order to make a new bid"
          (is (auction-offering/bid! {:offering/address offering}
                                     {:value (web3/to-wei 0.3 :ether)
                                      :from addr1})))
        (testing "State of the auction offering is correct"
          (is (= {:auction-offering/min-bid-increase 100000000000000000
                  :auction-offering/winning-bidder addr1
                  :auction-offering/bid-count 3}
                 (select-keys (auction-offering/get-auction-offering offering)
                              [:auction-offering/min-bid-increase
                               :auction-offering/winning-bidder
                               :auction-offering/bid-count]))))))))


(deftest create-subdomain-auction-offering
  (let [[addr0 addr1 addr2 addr3] (my-addresses)
        t0 (to-epoch (t/plus (now) (t/weeks 2)))]

    (is (registrar/register! {:ens.record/label "tld"}
                             {:from addr0}))
    (is (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                 :ens.record/node "tld.eth"
                                 :ens.record/owner addr1}
                                {:from addr0}))
    #_(is (= addr1 (ens/owner {:ens.record/node (namehash
                                                  "theirsub.tld.eth")})))
    (testing "Offering the name for a bid"
      (let [tx-hash (auction-offering-factory/create-offering!
                      {:offering/name "theirsub.tld.eth"
                       :offering/price (eth->wei 0.1)
                       :auction-offering/end-time t0
                       :auction-offering/extension-duration (t/in-seconds (t/days 4))
                       :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                      {:from addr1})]

        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "theirsub.tld.eth")
                                                                  :from-block 0
                                                                  :owner addr1})]
          (testing "on-offering event should fire"
            (is (not (nil? offering))))

          (testing "Can't place a bid before ownership transfer"
            (is (thrown? :default (auction-offering/bid! {:offering/address offering}
                                                         {:value (web3/to-wei 0.1 :ether)
                                                          :from addr1}))))

          (testing "Transferrnig ownership to the offer"
            (is (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                         :ens.record/node "tld.eth"
                                         :ens.record/owner offering}
                                        {:from addr0})))

          (testing "Can place a proper bid"
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value (web3/to-wei 0.1 :ether)
                                        :from addr2})))

          (web3-evm/increase-time! @web3 [(t/in-seconds (t/days 13))])

          (let [balance-of-2 (balance addr2)]
            (testing "Can place a bid"
              (is (auction-offering/bid! {:offering/address offering}
                                         {:value (web3/to-wei 0.3 :ether)
                                          :from addr3})))

            (testing "User who was overbid, should have his funds back from auction offering."

              (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                        (balance addr2))
                     100000)))

            (testing "Nothing to withdraw if return transfer succeeded on overbid."
              (is (auction-offering/withdraw! {:offering offering
                                               :address addr2}
                                              {:from addr2}))
              (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                        (balance addr2))
                     100000)))

            (testing "State of the auction offering is correct"
              (is (< (- (:auction-offering/end-time (auction-offering/get-auction-offering offering))
                        t0
                        (t/in-seconds (t/days 3)))
                     10)))                                  ;;threashold on operations
            ))))))


(deftest subdomain-auction-withdraw
  (let [[addr0 addr1 addr2 addr3] (my-addresses)
        t0 (to-epoch (t/plus (now) (t/weeks 2)))]

    (is (registrar/register! {:ens.record/label "tld"}
                             {:from addr0}))

    (is (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                 :ens.record/node "tld.eth"
                                 :ens.record/owner addr1}
                                {:from addr0}))

    (testing "Offering the name for a bid"
      (let [tx-hash (auction-offering-factory/create-offering!
                      {:offering/name "theirsub.tld.eth"
                       :offering/price (eth->wei 0.1)
                       :auction-offering/end-time t0
                       :auction-offering/extension-duration (t/in-seconds (t/days 4))
                       :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                      {:from addr1})]

        (is tx-hash)

        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "theirsub.tld.eth")
                                                                  :from-block 0
                                                                  :owner addr1})]
          (testing "on-offering event should fire"
            (is (not (nil? offering))))

          (testing "Transferrnig ownership to the offer"
            (is (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                         :ens.record/node "tld.eth"
                                         :ens.record/owner offering}
                                        {:from addr0})))

          (testing "Can place a proper bid"
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value (web3/to-wei 0.1 :ether)
                                        :from addr2})))

          (web3-evm/increase-time! @web3 [(t/in-seconds (t/days 13))])

          (let [balance-of-2 (balance addr2)]
            (testing "Can place a bid"
              (is (auction-offering/bid! {:offering/address offering}
                                         {:value (web3/to-wei 0.3 :ether)
                                          :from addr3})))

            (testing "Emergency address can withdraw funds to a user's address on his behalf."
              (is (auction-offering/withdraw! {:offering offering
                                               :address addr2}
                                              {:from addr0}))

              (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                        (balance addr2))
                     100000)))

            (testing "user can't withdraw twice."
              (is (auction-offering/withdraw! {:offering offering
                                               :address addr2}
                                              {:from addr2}))

              (is (< (- (.plus balance-of-2 (web3/to-wei 0.1 :ether))
                        (balance addr2))
                     100000)))

            (testing "State of the auction offering is correct"
              (is (< (- (:auction-offering/end-time (auction-offering/get-auction-offering offering))
                        t0
                        (t/in-seconds (t/days 3)))
                     10)))                                  ;;threashold on operations
            ))))))

(deftest freezed-auction-offering-behaviour
  (let [[addr0 addr1 addr2 addr3] (my-addresses)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr1})))

    (testing "Offering the name for a bid"
      (let [tx-hash (auction-offering-factory/create-offering!
                      {:offering/name "abc.eth"
                       :offering/price (eth->wei 0.1)
                       :auction-offering/end-time (to-epoch (t/plus (now) (t/weeks 2)))
                       :auction-offering/extension-duration 0
                       :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                      {:from addr1})]
        (is tx-hash)

        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "abc.eth")
                                                                  :from-block 0
                                                                  :owner addr1})]
          (testing "on-offering event should fire"
            (is (not (nil? offering))))

          (testing "Transferrnig ownership to the offer"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr1})))
          (testing "Emergency multisig can pause the registry"
            (is (offering-registry/emergency-pause! {:from addr0})))
          (testing "Can't place a bid, while the registry is paused "
            (is (thrown? :default (auction-offering/bid! {:offering/address offering}
                                                         {:value (web3/to-wei 0.1 :ether)
                                                          :from addr2}))))
          (testing "Emergency multisig can release the registry"
            (is (offering-registry/emergency-release! {:from addr0})))
          (testing "Can place a bid, whe it's resumed "
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value (web3/to-wei 0.1 :ether)
                                        :from addr2})))
          (web3-evm/increase-time! @web3 [(t/in-seconds (t/days 15))])

          (testing "Emergency multisig can pause the registry"
            (is (offering-registry/emergency-pause! {:from addr0})))
          (testing "Finalizing is not possible on frozen registry"
            (is (thrown? :default (auction-offering/finalize! offering {:from addr1}))))
          (testing "Emergency multisig can release the registry"
            (is (offering-registry/emergency-release! {:from addr0})))
          (testing "Finalizing works when it's time"
            (is (auction-offering/finalize! offering {:from addr1}))))))))

(deftest freezed-buy-now-offering-behaviour
  (let [[addr0 addr1 addr2 addr3] (my-addresses)]
    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr1})))

    (testing "Making an instant offer"
      (let [tx-hash (buy-now-offering-factory/create-offering! {:offering/name "abc.eth"
                                                                :offering/price (eth->wei 0.1)}
                                                               {:from addr1})]
        (is tx-hash)

        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "abc.eth")
                                                                  :from-block 0
                                                                  :owner addr1})]
          (testing "Transferrnig ownership to the offering"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr1})))

          (testing "Emergency multisig can pause the registry"
            (is (offering-registry/emergency-pause! {:from addr0})))

          (testing "Offering can't be bought while registry is frozen"
            (is (thrown? :default (buy-now-offering/buy! {:offering/address offering}
                                                         {:value (eth->wei 0.1)
                                                          :from addr2}))))

          (testing "Emergency multisig can release the registry"
            (is (offering-registry/emergency-release! {:from addr0})))

          (testing "Offering accepts the exact value"
            (is (buy-now-offering/buy! {:offering/address offering}
                                       {:value (eth->wei 0.1)
                                        :from addr2}))))))))