(ns name-bazaar.ui.spec
  (:require
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3? date?]]
    [district0x.ui.spec]))

(s/def :offering/address address?)
(s/def :offering/node sha3?)
(s/def :offering/name string?)
(s/def :offering/original-owner address?)
(s/def :offering/new-owner (s/nilable address?))
(s/def :offering/version int?)
(s/def :offering/type keyword?)
(s/def :offering/created-on date?)
(s/def :offering/transferred-on (s/nilable date?))
(s/def :offering/price not-neg?)
(s/def :auction-offering/end-time date?)
(s/def :auction-offering/extension-duration not-neg?)
(s/def :auction-offering/min-bid-increase not-neg?)
(s/def :auction-offering/highest-bidder address?)
(s/def :auction-offering/bid-count not-neg?)

(s/def :offering-registry/offering (s/keys :opt [:offering/node
                                                 :offering/name
                                                 :offering/original-owner
                                                 :offering/new-owner
                                                 :offering/version
                                                 :offering/type
                                                 :offering/created-on
                                                 :offering/transferred-on
                                                 :offering/price
                                                 :auction-offering/end-time
                                                 :auction-offering/extension-duration
                                                 :auction-offering/min-bid-increase
                                                 :auction-offering/highest-bidder
                                                 :auction-offering/bid-count]))

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

(s/def :watched-names/ens-records (s/coll-of (s/keys :req [:ens.record/name :ens.record/node])))
(s/def :watched-names/new-name string?)

(s/def :search-form.watched-names/data (s/keys :opt [:watched-names/ens-records
                                                     :watched-names/new-name]))

(s/def :search-form/watched-names (s/merge :db/form
                                           (s/keys :opts-un [:search-form.watched-names/data])))

(s/def :search-form.search-offerings/data (s/keys :opt [:offering/name
                                                        :offering/min-price
                                                        :offering/max-price
                                                        :offering/max-end-time
                                                        :offering/version
                                                        :offering/node-owner?]
                                                  :opt-un [:search-params/offset
                                                           :search-params/limit
                                                           :search-params/order-by]))
(s/def :search-form/search-offerings (s/merge :db/form
                                              (s/keys :opts-un [:search-form.search-offerings/data])))

(s/def :search-form.home-page-search/data (s/keys :opt [:offering/name
                                                        :offering/node-owner?]
                                                  :opt-un [:search-params/order-by
                                                           :search-params/limit]))
(s/def :search-form/home-page-search (s/merge :db/form
                                              (s/keys :opts-un [:search-form.home-page-search/data])))


(s/def :search-form.search-offering-requests/data (s/keys :opts [:offering-request/name]
                                                          :opt-un [:search-params/offset :search-params/order-by]))
(s/def :search-form/search-offering-requests (s/merge :db/form
                                                      (s/keys :opts-un [:search-form.search-offering-requests/data])))


(s/def :search-params/offerings (s/merge :search-form.search-offerings/data (s/keys :opt [:offering/original-owner])))
(s/def :search-results/offerings (s/map-of :search-params/offerings :db/search-results))

(s/def :search-params/offering-requests :search-form/search-offering-requests)
(s/def :search-results/offering-requests (s/map-of :search-params/offering-requests :db/search-results))

(s/def :form.ens/set-owner (s/map-of (s/keys :req [:ens.record/node]) :db/form))
(s/def :form.buy-now-offering-factory/create-offering :db/form)
(s/def :form.buy-now-offering/buy :db/contract-address-id-form)
(s/def :form.buy-now-offering/set-settings :db/contract-address-id-form)
(s/def :form.auction-offering-factory/create-offering :db/form)
(s/def :form.auction-offering/bid :db/contract-address-id-form)
(s/def :form.auction-offering/finalize :db/contract-address-id-form)
(s/def :form.auction-offering/withdraw :db/contract-address-id-form)
(s/def :form.auction-offering/set-settings :db/contract-address-id-form)
(s/def :form.offering/reclaim-ownership :db/contract-address-id-form)
(s/def :form.offering-requests/add-request (s/map-of (s/keys :req [:offering-request/name]) :db/form))

(s/def :name-bazaar.ui.db/db (s/merge
                               :district0x.ui/db
                               (s/keys :req [:ens/records
                                             :offering-registry/offerings
                                             :offering-requests/requests

                                             :form.ens/set-owner
                                             :form.buy-now-offering-factory/create-offering
                                             :form.buy-now-offering/buy
                                             :form.buy-now-offering/set-settings
                                             :form.auction-offering-factory/create-offering
                                             :form.auction-offering/bid
                                             :form.auction-offering/finalize
                                             :form.auction-offering/withdraw
                                             :form.auction-offering/set-settings
                                             :form.offering/reclaim-ownership
                                             :form.offering-requests/add-request
                                             :form.district0x-emails/set-email

                                             :search-results/offerings
                                             :search-results/offering-requests

                                             :search-form/search-offerings
                                             :search-form/search-offering-requests
                                             :search-form/home-page-search
                                             :search-form/watched-names

                                             ])))