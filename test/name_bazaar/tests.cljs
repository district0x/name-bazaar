(ns name-bazaar.tests
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as time]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth :refer [contract-call]]
    [cljs-web3.evm :as web3-evm]
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district0x.big-number :as bn]
    [district0x.test.contracts :as test-utils :refer [state-call! now-plus-seconds get-balance-ch contract-call-ch state-call-ch! state-call-ch! increase-time-and-mine-ch! mine-ch!]]
    [district0x.test.node :refer [fetch-contract fetch-bin fetch-abi]]
    [district0x.utils :as u :refer [wei->eth->num eth->wei->num]]
    [print.foo :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def accounts-secrets ["0x2331c9b2390ff4acfd9bb1277f35cb303581bf8d30433aec9d517dd7c6213e05"
                       "0x862b9e4ac273b75153122c16611bb73b85ee35d2443f49b6651dde9b624d3175"
                       "0xfe7a2920e0b7572ee98e78b4a551723697c1678bed60d2c7ddd8d015ea15f241"
                       "0x84aae7d13483fc0ef0910cae872a15a10f60a495b7067793bc868250d1c760ba"
                       "0x4919c9259eac34d6af231edf36af697f7a6291bc28c9837f7fabb678e750fb98"
                       "0xbad6bfea21ca0d5d9e02f8a6854a5d6aea31fc57505ce336ea7ebbf31e91f487"
                       "0xd804f2fe99d8186aa9dc2a6e74c0c4a551b12d29db9bba79187664b86a830659"
                       "0x564af4b4727963fd3830abfdef5f97ba2b06926760aaff5869e6f8f4ce6e2538"
                       "0x5a157c8984c783fe9e1fc33f8b25e5a2b9d46b78b06e8d0081d594483fb52a52"
                       "0xe80a3596cd9c6daaa66007e742d5fc07a7cb0d139f2a904a218edb14d47e7de2"
                       "0x7bb136a6ec7bbe915bf8acc42ce0625936c5ddf970c23953b81f5304ace40c08"
                       "0xc65d6da272a181baa120e492e69bb94a4cfc70188fbe97cd46359c0f35a78f92"
                       "0x433fdca0953ec0b358178085e74d108587374f4d8b823cf93e3c46685981b6a6"
                       "0x99465e2dc764d25d60f76c0f3f7857bfacd873c8958cc24bf5361e1f57abc1ea"
                       "0xbd410cd7cfe6209e37b37ca3dd316b63971633a7e3a6c4781137df35fae5c23a"])

(def accounts ["0x76852ba1f2fcb6355c57ffbd0bccfc226411f611"
               "0xe5e0c25e7928ddb94a5a9569e25bdb1c5ef68392"
               "0x7d3a19dc294eb5043e019b72adae56d67eaa4b07"
               "0xd43c63869ea4c4a73efe218321a4f0653304026f"
               "0xb2165ba402f5625ba5d59bc432b79bcc342250ae"
               "0x979582a341f47a4caeac6cefb1a3af123a939264"
               "0xb66b622f6ee6cc98581ed03ea2a393cb2de7a33e"
               "0x6e17baf999bb6a6204d80d55a3e0910e7d4eceed"
               "0x85ebe786470fd4204b307f39f49724136fcd2a2c"
               "0x46f9d973f77133ecbfc0d1f43de3c00147a82096"
               "0x0cf38f282e4cd051ca637115f745a8f60cbd215e"
               "0xebbf7c3882b86f5eb4f13dbaed2487837f6ce786"
               "0x4ee964fa20f7f3195b34a2d76579ced86a3de171"
               "0xdd88343d4760e872d86a309c6cd3a4ed247fd335"
               "0x1a1191cea03f9a15159f77e5e50f221c2b4b7eff"])

(def Web3 (js/require "web3"))
(def TestRPC (js/require "ethereumjs-testrpc"))
(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def normalize (aget (js/require "eth-ens-namehash") "normalize"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))
(set! js/Web3 Web3)

(def web3 (new Web3))

(def ^:dynamic ENS nil)
(def ^:dynamic OfferingRegistry nil)
(def ^:dynamic OfferingRequests nil)
(def ^:dynamic ENSNodeNames nil)
(def ^:dynamic InstantBuyOfferingFactory nil)
(def ^:dynamic EnglishAuctionOfferingFactory nil)
(def instant-buy-offering-abi (fetch-abi "InstantBuyOffering"))
(def english-auction-offering-abi (fetch-abi "EnglishAuctionOffering"))

(defn instant-buy-offering-at [contract-address]
  (web3-eth/contract-at web3 instant-buy-offering-abi contract-address))

(defn english-auction-offering-at [contract-address]
  (web3-eth/contract-at web3 english-auction-offering-abi contract-address))

(defn deploy-contracts! [done]
  (go
    (.setProvider web3 (.provider TestRPC (clj->js {:accounts (->> accounts-secrets
                                                                (map (fn [secret]
                                                                       {:balance (web3/to-wei 0x64 :ether)
                                                                        :secretKey secret})))
                                                    :locked false})))

    (set! ENS (<! (test-utils/deploy-contract-ch!
                    {:web3 web3
                     :from (first accounts)
                     :abi (fetch-abi "ENS")
                     :bin (fetch-bin "ENS")
                     :args []})))

    (set! OfferingRegistry (<! (test-utils/deploy-contract-ch!
                                 {:web3 web3
                                  :from (first accounts)
                                  :abi (fetch-abi "OfferingRegistry")
                                  :bin (fetch-bin "OfferingRegistry")
                                  :args []})))

    (set! ENSNodeNames (<! (test-utils/deploy-contract-ch!
                             {:web3 web3
                              :from (first accounts)
                              :abi (fetch-abi "ENSNodeNames")
                              :bin (fetch-bin "ENSNodeNames")
                              :args []})))

    (set! OfferingRequests (<! (test-utils/deploy-contract-ch!
                                 {:web3 web3
                                  :from (first accounts)
                                  :abi (fetch-abi "OfferingRequests")
                                  :bin (fetch-bin "OfferingRequests")
                                  :args [(aget ENSNodeNames "address")]})))

    (set! InstantBuyOfferingFactory (<! (test-utils/deploy-contract-ch!
                                          {:web3 web3
                                           :from (first accounts)
                                           :abi (fetch-abi "InstantBuyOfferingFactory")
                                           :bin (fetch-bin "InstantBuyOfferingFactory")
                                           :args (map #(aget % "address")
                                                      [ENS OfferingRegistry OfferingRequests])})))

    (set! EnglishAuctionOfferingFactory (<! (test-utils/deploy-contract-ch!
                                              {:web3 web3
                                               :from (first accounts)
                                               :abi (fetch-abi "EnglishAuctionOfferingFactory")
                                               :bin (fetch-bin "EnglishAuctionOfferingFactory")
                                               :args (map #(aget % "address")
                                                          [ENS OfferingRegistry OfferingRequests])})))

    (doseq [Contract [OfferingRegistry OfferingRequests]]
      (<! (state-call-ch! Contract :set-factories
                          {:args [(map #(aget % "address") [InstantBuyOfferingFactory EnglishAuctionOfferingFactory])
                                  true]
                           :from (first accounts)})))

    (done)))

(use-fixtures
  :each
  {:before
   (fn []
     (async done
       (deploy-contracts! done)))
   :after
   (fn []
     (async done (js/setTimeout #(done) 0)))
   })

(deftest name-bazaar-test1
  (is InstantBuyOfferingFactory)
  (async done
    (go
      ;(is (= (namehash "") (<! (contract-call-ch Test :namehash ""))))
      ;(is (= (namehash "abc") (<! (contract-call-ch Test :namehash "abc"))))
      ;(is (= (namehash "foo.eth") (<! (contract-call-ch Test :namehash "foo.eth"))))
      ;(is (= (namehash "abc.def.ghi") (<! (contract-call-ch Test :namehash "abc.def.ghi"))))
      ;(is (= (namehash "123.abc.def.ghi") (<! (contract-call-ch Test :namehash "123.abc.def.ghi"))))
      ;(is (= (namehash "123-456.abc.def.ghi") (<! (contract-call-ch Test :namehash "123-456.abc.def.ghi"))))

      (testing "Can set ENS subnode owner"
        (is (true? (u/tx-address? (<! (state-call-ch! ENS :set-subnode-owner
                                                      {:args [(namehash "")
                                                              (sha3 "eth")
                                                              (first accounts)]
                                                       :from (first accounts)})))))

        (is (= (first accounts) (<! (contract-call-ch ENS :owner (namehash "eth"))))))

      (testing "Can create InstantBuy Offering"
        (is (true? (u/tx-address? (<! (state-call-ch! InstantBuyOfferingFactory :create-offering
                                                      {:args ["eth" 0]
                                                       :from (first accounts)})))))

        (let [offering-address (<! (contract-call-ch OfferingRegistry :offerings 0))]
          (is (web3/address? offering-address))
          (is (= (namehash "eth") (<! (contract-call-ch (instant-buy-offering-at offering-address) :node))))
          (is (= "eth" (<! (contract-call-ch (instant-buy-offering-at offering-address) :name))))
          (is (true? (u/tx-address? (<! (state-call-ch! ENS :set-owner
                                                        {:args [(namehash "eth") offering-address]
                                                         :from (first accounts)})))))
          (is (= offering-address (<! (contract-call-ch ENS :owner (namehash "eth")))))))
      (done))))
