(ns server.name-bazaar.smart-contract-altering-tests
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
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
       (mount/only [#'district.server.web3
                    #'district.server.smart-contracts/smart-contracts
                    #'name-bazaar.server.deployer/deployer])
       (mount/start)))
   :after
   (fn []
     (mount/stop)
     (async done (js/setTimeout #(done) 3000)))})

(deftest offering-reclaiming-buy-now-tld
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]
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
          (testing "on-offering event should fire"
            (is (not (nil? offering))))

          (testing "Transferrnig ownership to the offering"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr1})))

          (testing "The name ownership must be transferred to the offering"
            (is (= offering (ens/owner {:ens.record/node (namehash
                                                           "abc.eth")}))))

          (testing "Ensuring offering gets the deed"
            (is (= offering (registrar/entry-deed-owner {:ens.record/label "abc"}))))
          (testing "For Buy Now offering, original owner can reclaim ENS name ownership (for TLD also deed ownership)"
            (is (buy-now-offering/reclaim-ownership! offering {:from addr1})))

          (testing "The name ownership must be transferred back to owner"
            (is (= addr1 (ens/owner {:ens.record/node (namehash "abc.eth")}))))
          (testing "Ensuring the new owner gets back his deed"
            (is (= addr1 (registrar/entry-deed-owner {:ens.record/label "abc"})))))))))


(deftest offering-reclaiming-buy-now-subdomain
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]

    (is (registrar/register! {:ens.record/label "tld"}
                             {:from addr1}))
    (is (ens/set-subnode-owner!
          {:ens.record/label "theirsub"
           :ens.record/node "tld.eth"
           :ens.record/owner addr2}
          {:from addr1}))
    (testing "The name ownership must be transferred to the user"
      (is (= addr2 (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))
    (testing "Making an instant offer"
      (let [tx-hash (buy-now-offering-factory/create-offering! {:offering/name "theirsub.tld.eth"
                                                                :offering/price (eth->wei 0.1)}
                                                               {:from addr2})]
        (is tx-hash)

        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "theirsub.tld.eth")
                                                                  :from-block 0
                                                                  :owner addr2})]

          (testing "on-offering event should fire"
            (is (not (nil? offering))))

          (testing "Transferrnig ownership to the offering"
            (is (ens/set-owner! {:ens.record/node (namehash "theirsub.tld.eth")
                                 :ens.record/owner offering}
                                {:from addr2})))

          (testing "The name ownership must be transferred to the offering"
            (is (= offering (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))

          (testing "For Buy Now offering, original owner can reclaim ENS name ownership"
            (is (buy-now-offering/reclaim-ownership! offering
                                                     {:from addr2})))

          (testing "The name ownership must be transferred back to owner"
            (is (= addr2 (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")})))))))))



(deftest offering-reclaiming-auction-tld
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]

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
          (when offering
            (testing "Transferrnig ownership to the offering"
              (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                       {:from addr1})))

            (testing "The name ownership must be transferred to the offering"
              (is (= offering (ens/owner {:ens.record/node (namehash "abc.eth")}))))

            (testing "Ensuring offering gets the deed"
              (is (= offering (registrar/entry-deed-owner {:ens.record/label "abc"}))))
            (testing "For Buy Now offering, original owner can reclaim ENS name ownership (for TLD also deed ownership)"
              (is (buy-now-offering/reclaim-ownership! offering {:from addr1})))

            (testing "The name ownership must be transferred back to owner"
              (is (= addr1 (ens/owner {:ens.record/node (namehash "abc.eth")}))))
            (testing "Ensuring the new owner gets back his deed"
              (is (= addr1 (registrar/entry-deed-owner {:ens.record/label "abc"}))))))))))


(deftest offering-reclaiming-auction-subdomain
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]

    (is (registrar/register! {:ens.record/label "tld"}
                             {:from addr1}))

    (is (ens/set-subnode-owner!
          {:ens.record/label "theirsub"
           :ens.record/node "tld.eth"
           :ens.record/owner addr2}
          {:from addr1}))

    (testing "The name ownership must be transferred to the user"
      (is (= addr2 (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))

    (testing "Offering the name for a bid"
      (let [tx-hash (auction-offering-factory/create-offering!
                      {:offering/name "theirsub.tld.eth"
                       :offering/price (eth->wei 0.1)
                       :auction-offering/end-time (to-epoch (t/plus (now) (t/weeks 2)))
                       :auction-offering/extension-duration 0
                       :auction-offering/min-bid-increase (web3/to-wei 0.1 :ether)}
                      {:from addr2})]

        (is tx-hash)

        (let [{{:keys [:offering]} :args}
              (offering-registry/on-offering-added-in-tx tx-hash {:node (namehash "theirsub.tld.eth")
                                                                  :from-block 0
                                                                  :owner addr2})]
          (testing "on-offering event should fire"
            (is (not (nil? offering))))
          (when offering
            (testing "Transferrnig ownership to the offering"
              (is (ens/set-subnode-owner! {:ens.record/label "theirsub"
                                           :ens.record/node "tld.eth"
                                           :ens.record/owner offering}
                                          {:from addr1})))

            (testing "The name ownership must be transferred to the offering"
              (is (= offering (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))

            (testing "For Buy Now offering, original owner can reclaim ENS name ownership"
              (is (buy-now-offering/reclaim-ownership! offering {:from addr2})))

            (testing "The name ownership must be transferred back to owner"
              (is (= addr2 (ens/owner {:ens.record/node (namehash "theirsub.tld.eth")}))))))))))


(deftest offering-reclaiming-auction-tld-emergency
  (let [[addr0 addr1 addr2 addr3 addr4] (web3-eth/accounts @web3)]

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

          (testing "Transferrnig ownership to the offering"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr1})))

          (testing "The name ownership must be transferred to the offering"
            (is (= offering (ens/owner {:ens.record/node (namehash "abc.eth")}))))

          (testing "Ensuring offering gets the deed"
            (is (= offering (registrar/entry-deed-owner {:ens.record/label "abc"}))))

          (testing "Can place a proper bid"
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value (web3/to-wei 0.1 :ether)
                                        :from addr4})))

          (let [balance-of-4 (web3-eth/get-balance @web3 addr4)]
            (testing "For Buy Now offering, original owner can reclaim ENS name ownership (for TLD also deed ownership)"
              (is (buy-now-offering/reclaim-ownership! offering {:from addr0})))

            (testing "The name ownership must be transferred back to owner"
              (is (= addr1 (ens/owner {:ens.record/node (namehash "abc.eth")}))))
            (testing "Ensuring the new owner gets back his deed"
              (is (= addr1 (registrar/entry-deed-owner {:ens.record/label "abc"}))))

            (testing "User who was overbid, can successfully withdraw funds from auction offering."
              (is (auction-offering/withdraw! {:offering offering
                                               :address addr4}
                                              {:from addr4}))
              (is (< (- (bn/+ balance-of-4 (web3/to-wei 0.1 :ether))
                        (web3-eth/get-balance @web3 addr4))
                     100000)))))))))


(deftest offering-editing-buy-now
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]
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
          (testing "on-offering event should fire"
            (is (not (nil? offering))))

          (testing "Transferrnig ownership to the offering"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr1})))

          (testing "Offering can be successfully edited by original owner, throws error if different address tries to edit"
            (is (thrown? :default (buy-now-offering/set-settings! {:offering/address offering
                                                                   :offering/price (eth->wei 0.2)}
                                                                  {:from addr2}))))
          (testing "Updating price"
            (is (buy-now-offering/set-settings! {:offering/address offering
                                                 :offering/price (eth->wei 0.2)}
                                                {:from addr1})))
          (testing "The price is updated"
            (is (= (eth->wei 0.2) (str (:offering/price (offering/get-offering offering)))))))))))

(deftest offering-editing-auction
  (let [[addr0 addr1 addr2 addr3 addr4] (web3-eth/accounts @web3)]
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

          (testing "Transferrnig ownership to the offering"
            (is (registrar/transfer! {:ens.record/label "abc" :ens.record/owner offering}
                                     {:from addr1})))


          (let [t0 (to-epoch (t/plus (now) (t/weeks 4)))]
            (testing "Auction offering can be edited"
              (is (auction-offering/set-settings!
                    {:offering/address offering
                     :offering/price (eth->wei 0.2)
                     :auction-offering/end-time t0
                     :auction-offering/extension-duration 10000
                     :auction-offering/min-bid-increase (web3/to-wei 0.2 :ether)}
                    {:from addr1})))


            (testing "State of the auction offering is correct"
              (is (= {:auction-offering/end-time (js/Math.floor t0),
                      :auction-offering/extension-duration 10000,
                      :auction-offering/min-bid-increase 200000000000000000}
                     (select-keys (auction-offering/get-auction-offering offering)
                                  [:auction-offering/end-time
                                   :auction-offering/extension-duration
                                   :auction-offering/min-bid-increase])))))

          (testing "The price is updated"
            (is (= (eth->wei 0.2) (str (:offering/price (offering/get-offering offering))))))

          (testing "Can place a proper bid"
            (is (auction-offering/bid! {:offering/address offering}
                                       {:value (web3/to-wei 0.4 :ether)
                                        :from addr4})))

          (testing "Auction offering can be edited only when has 0 bids, throws error if otherwise."
            (is (thrown? :default (auction-offering/set-settings!
                                    {:offering/address offering
                                     :offering/price (eth->wei 0.8)
                                     :auction-offering/end-time (to-epoch (t/plus (now) (t/weeks 8)))
                                     :auction-offering/extension-duration 20000
                                     :auction-offering/min-bid-increase (web3/to-wei 0.3 :ether)}
                                    {:from addr1})))))))))

(deftest offering-changing-register-and-requests
  (let [[addr0 addr1 addr2 addr3] (web3-eth/accounts @web3)]

    (testing "Registering name"
      (is (registrar/register! {:ens.record/label "abc"}
                               {:from addr1})))
    (testing "user can successfully request ENS name"
      (is (offering-requests/add-request! {:offering-request/name "abc.eth"}
                                          {:form addr1})))

    (testing "and it increase counter of requests."
      (is (= 1 (:offering-request/requesters-count
                 (offering-requests/get-request {:offering-request/node (namehash "abc.eth")})))))

    (testing "Making an instant offer"
      (is (buy-now-offering-factory/create-offering! {:offering/name "abc.eth"
                                                      :offering/price (eth->wei 0.1)}
                                                     {:from addr1})))

    (testing "if offering is created after that, it zeroes requests counter"
      (is (= 0 (:offering-request/requesters-count
                 (offering-requests/get-request {:offering-request/node (namehash "abc.eth")})))))))
