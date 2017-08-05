(ns name-bazaar.ui.db
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.spec.alpha :as s]
    [district0x.ui.db]
    [district0x.shared.utils :as d0x-shared-utils :refer [sha3? address? date? not-neg?]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [re-frame.core :refer [dispatch]]))

(s/def :offering/address address?)
(s/def :offering/node sha3?)
(s/def :offering/name string?)
(s/def :offering/original-owner address?)
(s/def :offering/new-owner address?)
(s/def :offering/version int?)
(s/def :offering/type keyword?)
(s/def :offering/created-on date?)
(s/def :offering/transferred-on date?)
(s/def :offering/price not-neg?)
(s/def :english-auction-offering/end-time date?)
(s/def :english-auction-offering/extension-duration not-neg?)
(s/def :english-auction-offering/min-bid-increase not-neg?)
(s/def :english-auction-offering/highest-bidder address?)
(s/def :english-auction-offering.bid/amount not-neg?)
(s/def :english-auction-offering.bid/updated-on date?)
(s/def :english-auction-offering/bid (s/keys :req [:english-auction-offering.bid/amount
                                                   :english-auction-offering.bid/updated-on]))
(s/def :english-auction-offering/bids (s/map-of address? :english-auction-offering/bid))

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

(s/def :offering-registry/offerings (s/map-of :offering/address :offering-registry/offering))

(s/def :offering-request/requesters (s/coll-of address? :kind set?))
(s/def :offering-request/requesters-count not-neg?)

(s/def :offering-requests/request (s/keys :opt [:offering-request/requesters-count
                                                :offering-request/requesters]))

(s/def :offering-requests/requests (s/map-of sha3? :offering-requests/request))

(s/def :ens.record/node sha3?)
(s/def :ens.record/owner address?)
(s/def :ens.record/resolver address?)
(s/def :ens.record/ttl not-neg?)
(s/def :ens.record/name string?)

(s/def :ens/record (s/keys :opt [:ens.record/owner
                                 :ens.record/name
                                 :ens.record/resolver
                                 :ens.record/ttl]))

(s/def :ens/records (s/map-of :ens.record/node :ens/record))

(s/def ::watched-ens-records (s/coll-of (s/keys :req [:ens.record/name :ens.record/node]) :kind list?))

(s/def :search-form/offerings (s/keys :opt [:offering/name
                                            :offering/min-price
                                            :offering/max-price
                                            :offering/max-end-time
                                            :offering/version
                                            :offering/node-owner?]
                                      :opt-un [:district0x.ui.db/offset
                                               :district0x.ui.db/order-by]))

(s/def :search-form/offering-requests (s/keys :opts [:offering-request/name]
                                              :opt-un [:district0x.ui.db/offset :district0x.ui.db/order-by]))
(s/def :search-params/offerings (s/merge :search-form/offerings (s/keys :opt [:offering/original-owner])))
(s/def :search-results/offerings (s/map-of :search-params/offerings :district0x.ui.db/search-results))

(s/def :search-params/offering-requests :search-form/offering-requests)
(s/def :search-results/offering-requests (s/map-of :search-params/offering-requests :district0x.ui.db/search-results))

(s/def :form.ens/set-owner (s/map-of (s/keys :req [:ens.record/node]) :district0x.ui.db/form))
(s/def :form.instant-buy-offering-factory/create-offering :district0x.ui.db/nil-id-form)
(s/def :form.instant-buy-offering/buy :district0x.ui.db/contract-address-id-form)
(s/def :form.instant-buy-offering/set-settings :district0x.ui.db/contract-address-id-form)
(s/def :form.english-auction-offering-factory/create-offering :district0x.ui.db/nil-id-form)
(s/def :form.english-auction-offering/bid :district0x.ui.db/contract-address-id-form)
(s/def :form.english-auction-offering/finalize :district0x.ui.db/contract-address-id-form)
(s/def :form.english-auction-offering/withdraw :district0x.ui.db/contract-address-id-form)
(s/def :form.english-auction-offering/set-settings :district0x.ui.db/contract-address-id-form)
(s/def :form.offering/reclaim-ownership :district0x.ui.db/contract-address-id-form)
(s/def :form.offering-requests/add-request (s/map-of (s/keys :req [:offering-request/name]) :district0x.ui.db/form))

(s/def ::db (s/merge
              :district0x.ui.db/db
              (s/keys :req [:ens/records
                            :offering-registry/offerings
                            :offering-requests/requests

                            :form.ens/set-owner
                            :form.instant-buy-offering-factory/create-offering
                            :form.instant-buy-offering/buy
                            :form.instant-buy-offering/set-settings
                            :form.english-auction-offering-factory/create-offering
                            :form.english-auction-offering/bid
                            :form.english-auction-offering/finalize
                            :form.english-auction-offering/withdraw
                            :form.english-auction-offering/set-settings
                            :form.offering/reclaim-ownership
                            :form.offering-requests/add-request
                            :form.district0x-emails/set-email

                            :search-results/offerings
                            :search-results/offering-requests

                            :search-form/offerings
                            :search-form/offering-requests]
                      :req-un [::watched-ens-records])))



(def default-db
  (merge
    district0x.ui.db/default-db
    {:node-url #_"https://mainnet.infura.io/" "http://localhost:8549"
     :server-url "http://localhost:6200"
     :smart-contracts smart-contracts
     :contract-method-configs constants/contract-method-configs
     :form-configs constants/form-configs
     :form-field->query-param constants/form-field->query-param
     :route-handler->form-key constants/route-handler->form-key

     :offering-registry/offerings {}
     :offering-requests/requests {}
     :ens/records {}
     :watched-ens-records '()

     :form.ens/set-owner {}
     :form.instant-buy-offering-factory/create-offering {}
     :form.instant-buy-offering/buy {}
     :form.instant-buy-offering/set-settings {}
     :form.english-auction-offering-factory/create-offering {}
     :form.english-auction-offering/bid {}
     :form.english-auction-offering/finalize {}
     :form.english-auction-offering/withdraw {}
     :form.english-auction-offering/set-settings {}
     :form.offering/reclaim-ownership {}
     :form.offering-requests/add-request {}
     :form.district0x-emails/set-email {}

     :search-results/offerings {}
     :search-results/offering-requests {}

     :search-form/offerings {:offering/node-owner? true :order-by {:offering/created-on :desc}}
     :search-form/offering-requests {:order-by {:offering-request/requesters-count :desc}}

     }))