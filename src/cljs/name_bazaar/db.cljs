(ns name-bazaar.db
  (:require
    [cljs-time.core :as t]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-web3.core :as web3]
    [cljs.spec :as s]
    [name-bazaar.constants :as constants]
    [district0x.db]
    [district0x.utils :as u]
    [re-frame.core :refer [dispatch]]))

(s/def :form.instant-buy-offering-factory/create-offering ::district0x.db/submit-form)
(s/def :form.english-auction-offering-factory/create-offering ::district0x.db/submit-form)

(def default-db
  (merge
    district0x.db/default-db
    {:node-url #_"https://mainnet.infura.io/" "http://localhost:8549"
     :smart-contracts {:offering-registry {:name "OfferingRegistry" :address "0x0000000000000000000000000000000000000000"}
                       :offering-requests {:name "OfferingRequests" :address "0x0000000000000000000000000000000000000000"}
                       :instant-buy-offering-factory {:name "InstantBuyOfferingFactory" :address "0x0000000000000000000000000000000000000000"}
                       :instant-buy-offering {:name "InstantBuyOffering" :address "0x0000000000000000000000000000000000000000"}
                       :english-auction-offering-factory {:name "EnglishAuctionOfferingFactory" :address "0x0000000000000000000000000000000000000000"}
                       :english-auction-offering {:name "EnglishAuctionOffering" :address "0x0000000000000000000000000000000000000000"}
                       :ens-node-names {:name "ENSNodeNames" :dev-only? true :address "0x0000000000000000000000000000000000000000"}
                       :ens {:name "ENS" :dev-only? true :address "0x0000000000000000000000000000000000000000"}
                       :fifs-registrar {:name "FIFSRegistrar" :dev-only? true :address "0x0000000000000000000000000000000000000000"}}

     :form.instant-buy-offering-factory/create-offering
     {:loading? false
      :gas-limit 4500000
      :data {:instant-buy-offering-factory/name ""
             :instant-buy-offering-factory/price 0.01}
      :errors #{}}

     :form.english-auction-offering-factory/create-offering
     {:loading? false
      :gas-limit 2000000
      :data {:english-auction-offering-factory/name ""
             :english-auction-offering-factory/start-price 0.01
             :english-auction-offering-factory/start-time (to-epoch (t/now))
             :english-auction-offering-factory/end-time (to-epoch (t/plus (t/now) (t/weeks 1)))
             :english-auction-offering-factory/extension-duration (t/in-seconds (t/hours 1))
             :english-auction-offering-factory/extension-trigger-duration (t/in-seconds (t/hours 1))
             :english-auction-offering-factory/min-bid-increase 0.01}
      :errors #{}}}))