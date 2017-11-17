(ns name-bazaar.ui.spec
  (:require
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3? date?]]
    [district0x.ui.spec]))

(s/def :offering/address address?)
(s/def :offering/node sha3?)
(s/def :offering/name string?)
(s/def :offering/original-owner address?)
(s/def :offering/label string?)
(s/def :offering/label-hash sha3?)
(s/def :offering/new-owner (s/nilable address?))
(s/def :offering/version int?)
(s/def :offering/type keyword?)
(s/def :offering/created-on date?)
(s/def :offering/finalized-on (s/nilable date?))
(s/def :offering/price not-neg?)
(s/def :offering/auction? boolean?)
(s/def :offering/buy-now? boolean?)
(s/def :offering/name-level not-neg?)
(s/def :offering/top-level-name? boolean?)
(s/def :offering/label-length not-neg?)
(s/def :offering/contains-number? boolean?)
(s/def :offering/contains-special-char? boolean?)
(s/def :offering/contains-non-ascii? boolean?)
(s/def :auction-offering/end-time date?)
(s/def :auction-offering/extension-duration not-neg?)
(s/def :auction-offering/min-bid-increase not-neg?)
(s/def :auction-offering/highest-bidder address?)
(s/def :auction-offering/bid-count not-neg?)
(s/def :auction-offering/transfer-price? boolean?)
(s/def :auction-offering/bidder address?)
(s/def :auction-offering/pending-returns (s/map-of address? not-neg?))

(s/def ::offering (s/keys :opt [:offering/node
                                :offering/name
                                :offering/original-owner
                                :offering/new-owner
                                :offering/version
                                :offering/label
                                :offering/label-hash
                                :offering/type
                                :offering/auction?
                                :offering/buy-now?
                                :offering/created-on
                                :offering/finalized-on
                                :offering/price
                                :offering/name-level
                                :offering/top-level-name?
                                :offering/label-length
                                :offering/contains-number?
                                :offering/contains-special-char?
                                :offering/contains-non-ascii?
                                :auction-offering/end-time
                                :auction-offering/extension-duration
                                :auction-offering/min-bid-increase
                                :auction-offering/highest-bidder
                                :auction-offering/bid-count]))

(s/def ::offerings (s/map-of :offering/address ::offering))

(s/def :offering-request/requesters (s/coll-of address? :kind set?))
(s/def :offering-request/requesters-count not-neg?)
(s/def :offering-request/latest-round not-neg?)

(s/def :offering-requests/request (s/keys :opt [:offering-request/requesters-count
                                                :offering-request/requesters
                                                :offering-request/latest-round]))

(s/def ::offering-requests (s/map-of :ens.record/node :offering-requests/request))

(s/def :ens.record/node sha3?)
(s/def :ens.record/label string?)
(s/def :ens.record/label-hash sha3?)
(s/def :ens.record/owner address?)
(s/def :ens.record/name string?)
(s/def :ens.record/active-offering (s/nilable :offering/address))

(s/def :ens/record (s/keys :opt [:ens.record/owner
                                 :ens.record/label
                                 :ens.record/label-hash
                                 :ens.record/name
                                 :ens.record/active-offering]))

(s/def :ens/records (s/map-of :ens.record/node :ens/record))

(s/def :registrar.entry/state keyword)
(s/def :registrar.entry/registration-date (s/nilable date?))
(s/def :registrar.entry/value not-neg?)
(s/def :registrar.entry/highest-bid not-neg?)
(s/def :registrar.entry.deed/address address?)
(s/def :registrar.entry.deed/value not-neg?)
(s/def :registrar.entry.deed/owner address?)

(s/def :registrar/entry (s/keys :opt [:registrar.entry/state
                                      :registrar.entry/registration-date
                                      :registrar.entry/value
                                      :registrar.entry/highest-bid
                                      :registrar.entry.deed/address
                                      :registrar.entry.deed/value
                                      :registrar.entry.deed/owner]))

(s/def :registrar/entries (s/map-of :ens.record/label-hash :registrar/entry))

(s/def :watched-names/order (s/coll-of :ens.record/node))
(s/def ::watched-names (s/keys :req [:ens/records]
                               :req-un [:watched-names/order]))

(s/def ::search-results (s/map-of keyword? (s/map-of keyword? :db/search-results)))

(s/def ::offerings-main-search-drawer :db/drawer)
(s/def ::saved-searches (s/map-of keyword? (s/map-of string? string?)))


(s/def :infinite-list.expanded-item/height not-neg?)
(s/def :infinite-list/expanded-items (s/map-of any? (s/keys :req-un [:infinite-list.expanded-item/height])))
(s/def ::infinite-list (s/keys :req-un [:infinite-list/expanded-items]))

(s/def ::now date?)

(s/def :public-resolver.record/addr address?)
(s/def :public-resolver/record
  (s/keys :opt [:public-resolver.record/addr]))
(s/def :resolver-records/entry (s/map-of :ens.record/node :public-resolver/record))
(s/def :public-resolver/records
  (s/keys :opt [:resolver-records/entry]))

(s/def :public-resolver/reverse-record
  (s/keys :opt [:public-resolver.record/name]))
(s/def :resolver-records/reverse-entry (s/map-of address? :public-resolver/reverse-record))
(s/def :public-resolver/reverse-records
  (s/keys :opt [:resolver-records/reverse-entry]))

(s/def :offerings/total-count (s/nilable not-neg?))

(s/def :name-bazaar.ui.db/db (s/merge
                               :district0x.ui/db
                               (s/keys :req [:ens/records
                                             :registrar/entries]
                                       :req-un [::offerings
                                                ::offering-requests
                                                ::infinite-list
                                                ::watched-names
                                                ::now
                                                ::search-results
                                                ::saved-searches
                                                ::offerings-main-search-drawer]
                                       :opts [:offerings/total-count
                                              :public-resolver/records
                                              :public-resolver/reverse-records])))
