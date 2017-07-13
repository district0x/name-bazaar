(ns name-bazaar.server.dev
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.async.eth :as web3-eth]
    [cljs-web3.async.evm :as web3-evm]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state :refer [*server-state*]]
    [district0x.server.utils :as u]
    [district0x.server.utils :refer [watch-event-once]]
    [goog.date.Date]
    [name-bazaar.contracts-api.english-auction-offering-factory :as english-auction-offering-factory]
    [name-bazaar.contracts-api.ens :as ens]
    [name-bazaar.contracts-api.instant-buy-offering :as instant-buy-offering]
    [name-bazaar.contracts-api.instant-buy-offering-factory :as instant-buy-offering-factory]
    [name-bazaar.contracts-api.english-auction-offering :as english-auction-offering]
    [name-bazaar.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.contracts-api.used-by-factories :as used-by-factories]
    [name-bazaar.smart-contracts :refer [smart-contracts]]
    [print.foo :include-macros true]
    )
  (:require-macros [cljs.core.async.macros :refer [go]]))

(nodejs/enable-util-print!)

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))
(def Web3 (js/require "web3"))
(set! js/Web3 Web3)

(def total-accounts 10)

(defn on-jsload []
  (println "on-jsload"))

(defn -main [& _]
  (effects/create-testrpc-web3! *server-state* {:total_accounts total-accounts})
  (effects/load-smart-contracts! *server-state* smart-contracts)
  (go
    (<! (effects/load-my-addresses! *server-state*))))

(set! *main-cli-fn* -main)

(def default-deploy-opts
  {:from-index 0
   :contracts-file-namespace "name-bazaar.smart-contracts"
   :contracts-file-path "/src/shared/name_bazaar/smart_contracts.cljs"})

(def library-placeholders
  {:offering-library "__OfferingLibrary.sol:OfferingLibrary___"
   :instant-buy-offering-library "__instant_buy/InstantBuyOfferingLibrar__"
   :english-auction-offering-library "__english_auction/EnglishAuctionOfferi__"})

(defn deploy-ens! [server-state default-opts]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :ens})))

(defn deploy-offering-registry! [server-state default-opts]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :offering-registry})))

(defn deploy-offering-requests! [server-state default-opts]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :offering-requests})))

(defn deploy-offering-library! [server-state default-opts]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :offering-library})))

(defn deploy-instant-buy-library! [server-state default-opts]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :instant-buy-offering-library
                                                       :library-placeholders (select-keys library-placeholders
                                                                                          [:offering-library])})))

(defn deploy-english-auction-library! [server-state default-opts]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :english-auction-offering-library
                                                       :library-placeholders (select-keys library-placeholders
                                                                                          [:offering-library])})))

(defn deploy-instant-buy-factory! [server-state default-opts {:keys [:offering-factory/emergency-multisig]}]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :instant-buy-offering-factory
                                                       :library-placeholders (select-keys library-placeholders
                                                                                          [:offering-library
                                                                                           :instant-buy-offering-library])
                                                       :args [(state/contract-address server-state :ens)
                                                              (state/contract-address server-state :offering-registry)
                                                              (state/contract-address server-state :offering-requests)
                                                              emergency-multisig]})))

(defn deploy-english-auction-factory! [server-state default-opts {:keys [:offering-factory/emergency-multisig]}]
  (effects/deploy-smart-contract! server-state (merge default-opts
                                                      {:contract-key :english-auction-offering-factory
                                                       :library-placeholders (select-keys library-placeholders
                                                                                          [:offering-library
                                                                                           :english-auction-offering-library])
                                                       :args [(state/contract-address server-state :ens)
                                                              (state/contract-address server-state :offering-registry)
                                                              (state/contract-address server-state :offering-requests)
                                                              emergency-multisig]})))

(defn deploy-smart-contracts! [server-state]
  (let [ch (chan)]
    (go
      (<! (deploy-ens! server-state default-deploy-opts))
      (<! (deploy-offering-registry! server-state default-deploy-opts))
      (<! (deploy-offering-requests! server-state default-deploy-opts))
      (<! (deploy-offering-library! server-state default-deploy-opts))
      (<! (deploy-instant-buy-library! server-state default-deploy-opts))
      (<! (deploy-english-auction-library! server-state default-deploy-opts))
      (<! (deploy-instant-buy-factory! server-state default-deploy-opts {:offering-factory/emergency-multisig
                                                                         (state/active-address server-state)}))
      (<! (deploy-english-auction-factory! server-state default-deploy-opts {:offering-factory/emergency-multisig
                                                                             (state/active-address server-state)}))

      (<! (used-by-factories/set-factories! server-state {:contract-key :offering-registry}))
      (<! (used-by-factories/set-factories! server-state {:contract-key :offering-requests}))

      (effects/store-smart-contracts! (:smart-contracts @server-state)
                                      {:file-path (:contracts-file-path default-deploy-opts)
                                       :namespace (:contracts-file-namespace default-deploy-opts)})

      (>! ch server-state))
    ch))

(defn generate-data! [server-state]
  (let [ch (chan)
        root-node "eth"]
    (go
      (<! (ens/set-subnode-owner! server-state {:label root-node
                                                :owner (state/active-address server-state)}))

      (doseq [address-index (range total-accounts)]
        (let [owner (state/my-address server-state address-index)
              label (u/rand-str 5 {:lowercase-only? true})
              name (str label "." root-node)
              node (namehash name)
              offering-type (if (zero? (rand-int 2)) :instant-buy-offering :english-auction-offering)
              price (web3/to-wei (inc (rand-int 10)) :ether)
              buyer (state/my-address server-state (rand-int total-accounts))]
          (<! (ens/set-subnode-owner! server-state {:label label
                                                    :root-node root-node
                                                    :owner owner
                                                    :from (state/active-address server-state)}))
          (if (= offering-type :instant-buy-offering)
            (<! (instant-buy-offering-factory/create-offering! server-state
                                                               {:name name
                                                                :price price
                                                                :from owner}))
            (<! (english-auction-offering-factory/create-offering! server-state
                                                                   {:name name
                                                                    :start-price price
                                                                    :end-time (to-epoch (t/plus (t/now) (t/weeks 2)))
                                                                    :extension-duration (rand-int 10000)
                                                                    :min-bid-increase (web3/to-wei 1 :ether)
                                                                    :from owner})))


          (let [[_ {{:keys [:offering]} :args}] (<! (offering-registry/on-offering-added-once server-state
                                                                                              {:node node
                                                                                               :owner owner}))]
            (<! (ens/set-owner! server-state {:node node :owner offering :from owner}))

            (when (zero? (rand-int 2))
              (if (= offering-type :instant-buy-offering)
                (instant-buy-offering/buy! server-state {:contract-address offering
                                                         :value price
                                                         :from buyer})
                (english-auction-offering/bid! server-state {:contract-address offering
                                                             :value price
                                                             :from buyer})))

            )))




      #_(<! (instant-buy-offering/buy! server-state {:contract-address offering
                                                     :value-ether 0.01
                                                     :from (state/my-address server-state 1)}))


      #_(let [[_ {{:keys [:offering]} :args}]
              (<! (offering-registry/on-offering-added-once server-state
                                                            {:node (namehash "eth")
                                                             :owner (state/active-address server-state)}))]
          (<! (ens/set-owner! server-state {:name "eth" :owner offering}))

          (<! (instant-buy-offering/buy! server-state {:contract-address offering
                                                       :value-ether 0.01
                                                       :from (state/my-address server-state 1)}))

          )



      (>! ch true))
    ch))

(defn initialize! [server-state]
  (go
    (effects/load-smart-contracts! *server-state* smart-contracts)
    (<! (deploy-smart-contracts! server-state))
    (<! (generate-data! server-state))))

(comment
  (namehash "eth")
  (state/active-address)
  *server-state*
  (effects/create-testrpc-web3! *server-state* {:total_accounts total-accounts})
  (effects/load-smart-contracts! *server-state* smart-contracts)
  (deploy-smart-contracts! *server-state*)
  (state/web3)
  (deploy-smart-contracts! *server-state*)
  (initialize! *server-state*)
  (state/instance :ens)
  (state/contract-address :ens)
  (web3-eth/set-default-account! (state/web3) "0x1")
  (state/my-addresses)
  (go
    (print.foo/look (<! (web3-eth/accounts (state/web3)))))
  )

[nil ["0x56727ca3132d00307051a4fa6c6a2c3f07cb3f91"]]