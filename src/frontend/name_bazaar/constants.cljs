(ns name-bazaar.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]))

(def contracts-version "1.0.0")

(def contract-method-args-order
  {:instant-buy-offering-factory/create-offering [:offering/name
                                                  :offering/price]
   :instant-buy-offering/set-settings [:contract-address :offering/price]
   :instant-buy-offering/buy [:contract-address]
   :english-auction-offering-factory/create-offering [:offering/name
                                                      :offering/price
                                                      :english-auction-offering-factory/end-time
                                                      :english-auction-offering-factory/extension-duration
                                                      :english-auction-offering-factory/min-bid-increase]
   :english-auction-offering/set-settings [:contract-address
                                           :offering/price
                                           :english-auction-offering-factory/end-time
                                           :english-auction-offering-factory/extension-duration
                                           :english-auction-offering-factory/min-bid-increase]
   :english-auction-offering/bid [:contract-address]
   :english-auction-offering-factory/finalize [:contract-address
                                               :english-auction-offering/transfer-price?]
   :english-auction-offering-factory/withdraw [:contract-address
                                               :english-auction-offering/bidder]
   :ens/set-owner [:ens.record/node :ens.record/owner]})

(def contract-method-wei-args #{:offering/price
                                 :english-auction-offering-factory/min-bid-increase})

(def form-default-params
  {:instant-buy-offering-factory/create-offering
   {:offering/name ""
    :offering/price 0.01}

   :instant-buy-offering/set-settings
   {:offering/price 0}

   :english-auction-offering-factory/create-offering
   {:offering/name ""
    :offering/price 0.01
    :english-auction-offering-factory/end-time (to-epoch (t/plus (t/now) (t/weeks 1)))
    :english-auction-offering-factory/extension-duration (t/in-seconds (t/hours 1))
    :english-auction-offering-factory/min-bid-increase 0.01}

   :english-auction-offering/finalize
   {:english-auction-offering/transfer-price? true}

   :english-auction-offering/withdraw
   {:english-auction-offering/bidder nil}})

(def form-tx-opts
  {:ens/set-owner {:gas 100000}
   :instant-buy-offering-factory/create-offering {:gas 500000}
   :instant-buy-offering/buy {:gas 100000}
   :instant-buy-offering/set-settings {:gas 250000}
   :english-auction-offering-factory/create-offering {:gas 700000}
   :english-auction-offering/bid {:gas 70000}
   :english-auction-offering/finalize {:gas 70000}
   :english-auction-offering/withdraw {:gas 70000}
   :english-auction-offering/set-settings {:gas 1000000}
   :offering/reclaim-ownership {:gas 200000}})


(def library-placeholders
  {:offering-library "__OfferingLibrary.sol:OfferingLibrary___"
   :instant-buy-offering-library "__instant_buy/InstantBuyOfferingLibrar__"
   :english-auction-offering-library "__english_auction/EnglishAuctionOfferi__"})
