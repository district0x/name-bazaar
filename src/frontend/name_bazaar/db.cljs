(ns name-bazaar.db
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.spec.alpha :as s]
    [district0x.db]
    [district0x.utils :as u]
    [name-bazaar.constants :as constants]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [re-frame.core :refer [dispatch]]
    ))

(s/def :offering/node u/sha3?)
(s/def :offering/name string?)
(s/def :offering/original-owner u/address?)
(s/def :offering/new-owner u/address?)
(s/def :offering/version int?)
(s/def :offering/type keyword?)
(s/def :offering/created-on u/date?)
(s/def :offering/transferred-on u/date?)
(s/def :offering/price u/not-neg?)
(s/def :english-auction-offering/end-time u/date?)
(s/def :english-auction-offering/extension-duration u/not-neg?)
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
                                                 :offering/version
                                                 :offering/type
                                                 :offering/created-on
                                                 :offering/transferred-on
                                                 :offering/price
                                                 :english-auction-offering/end-time
                                                 :english-auction-offering/extension-duration
                                                 :english-auction-offering/min-bid-increase
                                                 :english-auction-offering/highest-bidder
                                                 :english-auction-offering/bids]))

(s/def :offering-registry/offerings (s/map-of u/address? :offering-registry/offering))

(s/def :offering-request/requesters (s/coll-of u/address? :kind set?))
(s/def :offering-request/requesters-count u/not-neg?)

(s/def :offering-requests/request (s/keys :opt [:offering-request/requesters-count
                                                :offering-request/requesters]))

(s/def :offering-requests/requests (s/map-of u/sha3? :offering-requests/request))

(s/def :ens.record/node u/sha3?)
(s/def :ens.record/owner u/address?)
(s/def :ens.record/resolver u/address?)
(s/def :ens.record/ttl u/not-neg?)
(s/def :ens.record/name string?)
(s/def :ens/record (s/keys :opt [:ens.record/owner
                                 :ens.record/name
                                 :ens.record/resolver
                                 :ens.record/ttl]))

(s/def :ens/records (s/map-of :ens.record/node :ens/record))

(s/def :instant-buy-offering-factory/create-offering :district0x.db/no-form-id-form)
(s/def :instant-buy-offering/buy ::contract-address-forms)

(s/def :english-auction-offering-factory/create-offering :district0x.db/no-form-id-form)

(s/def :ens/set-owner (s/map-of (s/merge :district0x.db/no-form-id
                                         (s/keys :req-un [:ens.record/node]))
                                :district0x.db/form))

(s/def :view-form/search-offerings (s/keys :opt [:offering/name
                                                 :offering/min-price
                                                 :offering/max-price
                                                 :offering/max-end-time
                                                 :offering/version
                                                 :offering/node-owner?]
                                           :opt-un [:district0x.db/offset
                                                    :district0x.db/order-by]))

(s/def :view/user-offerings (s/keys :opt [:offering/original-owner]))
(s/def :view-form/most-requested-names (s/keys :opt-un [:district0x.db/offset :district0x.db/order-by]))

(s/def :list-id/offerings (s/merge :view-form/search-offerings :view/user-offerings))
(s/def :list/offerings (s/map-of :list-id/offerings :district0x.db/list))
(s/def :list-id/offering-requests :view-form/most-requested-names)
(s/def :list/offering-requests (s/map-of :list-id/offering-requests :district0x.db/list))



(s/def :ens/set-owner (s/map-of (s/keys :req [:ens.record/node]) :district0x.db/form))
(s/def :instant-buy-offering-factory/create-offering (s/map-of :district0x.db/nil-form-id :district0x.db/form))
(s/def :instant-buy-offering/buy (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :instant-buy-offering/set-settings (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :english-auction-offering-factory/create-offering (s/map-of :district0x.db/nil-form-id :district0x.db/form))
(s/def :english-auction-offering/bid (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :english-auction-offering/finalize (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :english-auction-offering/withdraw (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :english-auction-offering/set-settings (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :offering/reclaim-ownership (s/map-of :district0x.db/contract-address-form-id :district0x.db/form))
(s/def :offering-requests/add-request (s/map-of (s/keys :req [:offering-request/name]) :district0x.db/form))

(s/def ::db (s/merge
              :district0x.db/db
              (s/keys :req [:ens/records
                            :offering-registry/offerings
                            :offering-requests/requests
                            :instant-buy-offering-factory/create-offering])))



(def default-db
  (merge
    district0x.db/default-db
    {:node-url #_"https://mainnet.infura.io/" "http://localhost:8549"
     :server-url "http://localhost:6200"
     :smart-contracts smart-contracts
     :contract-method-args-order constants/contract-method-args-order
     :contract-method-wei-args constants/contract-method-wei-args
     :form-default-params constants/form-default-params
     :form-tx-opts constants/form-tx-opts

     :offering-registry/offerings {}
     :offering-requests/requests {}
     :ens/records {}

     :ens/set-owner {}
     :instant-buy-offering-factory/create-offering {}
     :instant-buy-offering/buy {}
     :instant-buy-offering/set-settings {}
     :english-auction-offering-factory/create-offering {}
     :english-auction-offering/bid {}
     :english-auction-offering/finalize {}
     :english-auction-offering/withdraw {}
     :english-auction-offering/set-settings {}
     :offering/reclaim-ownership {}
     :offering-requests/add-request {}
     :district0x-emails/set-email {}

     :list/offerings {}
     :list/offering-requests {}

     :view-form/search-offerings {:offering/node-owner? true :order-by {:offering/created-on :desc}}
     :view-form/most-requested-names {:order-by {:offering-request/requesters-count :desc}}

     }))