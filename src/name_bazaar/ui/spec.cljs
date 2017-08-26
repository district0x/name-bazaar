(ns name-bazaar.ui.spec
  (:require
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3? date?]]
    [district0x.ui.spec]))

(s/def :offering/address address?)
(s/def :offering/node sha3?)
(s/def :offering/name string?)
(s/def :offering/registrar address?)
(s/def :offering/offering-registry address?)
(s/def :offering/emergency-multisig address?)
(s/def :offering/original-owner address?)
(s/def :offering/label-hash sha3?)
(s/def :offering/new-owner (s/nilable address?))
(s/def :offering/version int?)
(s/def :offering/type keyword?)
(s/def :offering/created-on date?)
(s/def :offering/transferred-on (s/nilable date?))
(s/def :offering/price not-neg?)
(s/def :offering/name-level not-neg?)
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

(s/def :offering-registry/offering (s/keys :opt [:offering/node
                                                 :offering/name
                                                 :offering/original-owner
                                                 :offering/new-owner
                                                 :offering/registrar
                                                 :offering/offering-registry
                                                 :offering/emergency-multisig
                                                 :offering/version
                                                 :offering/label-hash
                                                 :offering/type
                                                 :offering/created-on
                                                 :offering/transferred-on
                                                 :offering/price
                                                 :offering/name-level
                                                 :offering/label-length
                                                 :offering/contains-number?
                                                 :offering/contains-special-char?
                                                 :offering/contains-non-ascii?
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
(s/def :ens.record/label string?)
(s/def :ens.record/label-hash sha3?)
(s/def :ens.record/owner address?)
(s/def :ens.record/name string?)

(s/def :ens/record (s/keys :opt [:ens.record/owner
                                 :ens.record/label
                                 :ens.record/label-hash
                                 :ens.record/name]))

(s/def :ens/records (s/map-of :ens.record/node :ens/record))

(s/def :registrar.entry/state keyword)
(s/def :registrar.entry/registration-date date?)
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

(s/def :watched-names/ens-records (s/coll-of (s/keys :req [:ens.record/name :ens.record/node])))
(s/def :watched-names/new-name string?)

(s/def :search-results/offerings-main-search :db/search-results)
(s/def :search-results/offering-requests-main-search :db/search-results)
(s/def :search-results/name-offerings :db/search-results)
(s/def :search-results/user-offerings :db/search-results)
(s/def :search-results/similiar-offerings :db/search-results)
(s/def :search-results/bid-offerings :db/search-results)
(s/def :search-results/purchased-offerings :db/search-results)

(s/def ::offerings-search-params-drawer :db/drawer)
(s/def ::saved-searches (s/map-of keyword? (s/map-of string? string?)))


(s/def :infinite-list.expanded-item/height not-neg?)
(s/def :infinite-list/expanded-items (s/map-of integer? (s/keys :req-un [:infinite-list.expanded-item/height])))
(s/def ::infinite-list (s/keys :req-un [:infinite-list/expanded-items]))

(s/def :name-bazaar.ui.db/db (s/merge
                               :district0x.ui/db
                               (s/keys :req [:ens/records
                                             :offering-registry/offerings
                                             :offering-requests/requests

                                             :search-results/offerings-main-search
                                             :search-results/offering-requests-main-search
                                             :search-results/name-offerings
                                             :search-results/user-offerings
                                             :search-results/similiar-offerings
                                             :search-results/bid-offerings
                                             :search-results/purchased-offerings]
                                       :req-un [::infinite-list]
                                       :opt-un [::offerings-search-params-drawer
                                                ::saved-searches])))