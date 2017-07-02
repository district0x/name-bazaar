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

(s/def :offering/node u/address?)
(s/def :offering/name string?)
(s/def :offering/original-owner u/address?)
(s/def :offering/new-owner u/address?)
(s/def :offering/offering-type int?)
(s/def :offering/created-on u/date?)
(s/def :offering/transferred-on u/date?)
(s/def :instant-buy-offering/price u/not-neg?)
(s/def :english-auction-offering/start-price u/not-neg?)
(s/def :english-auction-offering/start-time u/date?)
(s/def :english-auction-offering/end-time u/date?)
(s/def :english-auction-offering/extension-duration u/not-neg?)
(s/def :english-auction-offering/extension-trigger-duration u/not-neg?)
(s/def :english-auction-offering/min-bid-increase u/not-neg?)
(s/def :english-auction-offering/highest-bidder u/address?)
(s/def :english-auction-offering.bid/amount u/not-neg?)
(s/def :english-auction-offering.bid/updated-on u/date?)
(s/def :english-auction-offering/bid (s/keys :req [:english-auction-offering.bid/amount
                                                   :english-auction-offering.bid/updated-on]))
(s/def :english-auction-offering/bids (s/map-of u/address? :english-auction-offering/bid))

(s/def :offering-registry/offering (s/keys :opt [:offering/node
                                                 :offering/name
                                                 :offering/original-owner
                                                 :offering/new-owner
                                                 :offering/offering-type
                                                 :offering/created-on
                                                 :offering/transferred-on
                                                 :instant-buy-offering/price
                                                 :english-auction-offering/start-price
                                                 :english-auction-offering/end-time
                                                 :english-auction-offering/extension-duration
                                                 :english-auction-offering/extension-trigger-duration
                                                 :english-auction-offering/min-bid-increase
                                                 :english-auction-offering/highest-bidder
                                                 :english-auction-offering/bids]))

(s/def :offering-registry/offerings (s/map-of u/address? :offering-registry/offering))

(s/def :request/requesters (s/coll-of u/address?))
(s/def :request/requesters-count u/non-neg?)
(s/def :request/name string?)

(s/def :offering-requests/request (s/keys :opt [:request/requesters-count
                                                :request/requesters
                                                :request/name]))

(s/def :offering-requests/requests (s/map-of u/address? :offering-requests/request))

(s/def :node/owner u/address?)
(s/def :node/name string?)
(s/def :node/offerings (s/coll-of u/address?))
(s/def :ens/node (s/keys :opt [:node/owner
                               :node/name
                               :node/offerings]))

(s/def :ens/nodes (s/map-of u/address? :ens/node))

(s/def :form.instant-buy-offering-factory/create-offering ::district0x.db/submit-form)
(s/def :form.english-auction-offering-factory/create-offering ::district0x.db/submit-form)
(s/def :form.ens/set-owner ::district0x.db/submit-form)

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
                       ;:ens-node-names {:name "ENSNodeNames" :dev-only? true :address "0x0000000000000000000000000000000000000000"}
                       :ens {:name "ENS" :dev-only? true :address "0x0000000000000000000000000000000000000000"}
                       ;:fifs-registrar {:name "FIFSRegistrar" :dev-only? true :address "0x0000000000000000000000000000000000000000"}
                       }

     :form.ens/set-owner
     {:loading? false
      :gas-limit 100000
      :data {:ens/node ""
             :ens/owner}
      :errors #{}}

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