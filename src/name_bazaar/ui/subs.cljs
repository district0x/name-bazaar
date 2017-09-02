(ns name-bazaar.ui.subs
  (:require
    [cemerick.url :as url]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [district0x.shared.utils :as d0x-shared-utils :refer [empty-address? zero-address?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [time-remaining]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [parse-query-params]]
    [re-frame.core :refer [reg-sub subscribe reg-sub-raw]]
    [reagent.ratom :refer [make-reaction]]))

(reg-sub
  :now
  (fn [db]
    (:now db)))

(reg-sub
  :offering-registry/offerings
  (fn [db]
    (:offering-registry/offerings db)))

(reg-sub
  :offering-registry/offering
  :<- [:offering-registry/offerings]
  (fn [offerings [_ offering-address]]
    (get offerings offering-address)))

(reg-sub
  :offering-requests/requests
  (fn [db]
    (:offering-requests/requests db)))

(reg-sub
  :ens/records
  (fn [db]
    (:ens/records db)))

(reg-sub
  :ens/record
  :<- [:ens/records]
  (fn [records [_ node]]
    (get records node)))

(reg-sub
  :ens.record/loaded?
  (fn [[_ node]]
    (subscribe [:ens/record node]))
  (fn [ens-record]
    (boolean (:ens.record/owner ens-record))))

(reg-sub
  :active-address-ens.record/owner?
  (fn [[_ node]]
    [(subscribe [:district0x/active-address])
     (subscribe [:ens/record node])])
  (fn [[active-address ens-record]]
    (= active-address (:ens.record/owner ens-record))))

(reg-sub
  :my-addresses-ens.record/owner?
  (fn [[_ node]]
    [(subscribe [:district0x/my-addresses])
     (subscribe [:ens/record node])])
  (fn [[my-addresses ens-record]]
    (contains? (set my-addresses) (:ens.record/owner ens-record))))

(reg-sub
  :registrar/entries
  (fn [db]
    (:registrar/entries db)))

(reg-sub
  :registrar/entry
  :<- [:registrar/entries]
  (fn [entries [_ label-hash]]
    (get entries label-hash)))

(reg-sub
  :registrar.entry.deed/loaded?
  (fn [[_ labe-hash]]
    (subscribe [:registrar/entry labe-hash]))
  (fn [registrar-entry]
    (boolean (or (zero-address? (:registrar.entry.deed/address registrar-entry))
                 (:registrar.entry.deed/value registrar-entry)))))

(reg-sub
  :active-address-registrar.entry.deed/owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/active-address])
     (subscribe [:registrar/entry label-hash])])
  (fn [[active-address registrar-entry]]
    (= active-address (:registrar.entry.deed/owner registrar-entry))))

(reg-sub
  :my-addresses-registrar.entry.deed/owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/my-addresses])
     (subscribe [:registrar/entry label-hash])])
  (fn [[my-addresses registrar-entry]]
    (contains? (set my-addresses) (:registrar.entry.deed/address registrar-entry))))

(reg-sub
  :offerings-search-params-drawer-open?
  (fn [db]
    (get-in db [:offerings-search-params-drawer :open?])))

(reg-sub
  :saved-searches
  (fn [db [_ saved-searches-key]]
    (get-in db [:saved-searches saved-searches-key])))

(reg-sub
  :saved-search-active?
  (fn [[_ saved-searches-key]]
    [(subscribe [:saved-searches saved-searches-key])
     (subscribe [:district0x/query-string])])
  (fn [[saved-searches query-string]]
    (boolean (get saved-searches query-string))))

#_(reg-sub
    :search-form/watched-names
    :<- [:district0x/db :search-form/watched-names]
    :<- [:ens/records]
    :<- [:offering-registry/offerings]
    (fn [watched-ens-records ens-records offerings]
      (map (fn [ens-record]
             (-> ens-record
               (merge (ens-records (:ens.record/node ens-record)))
               (update :ens.record/last-offering offerings)))
           watched-ens-records)))

(reg-sub
  :route-params/offering
  :<- [:district0x/route-params]
  :<- [:offering-registry/offerings]
  (fn [[route-params offerings]]
    (get-in offerings [(:offering/address route-params)])))

(reg-sub
  :offering/loaded?
  (fn [[_ offering-address]]
    (subscribe [:offering-registry/offering offering-address]))
  (fn [offering]
    (and (:offering/name offering)
         (or (= (:offering/type offering) :buy-now-offering)
             (:auction-offering/end-time offering)))))

(reg-sub
  :auction-offering/end-time-countdown
  (fn [[_ offering-address]]
    [(subscribe [:offering-registry/offering offering-address])
     (subscribe [:now])])
  (fn [[offering now]]
    (time-remaining now (:auction-offering/end-time offering))))

(reg-sub
  :auction-offering/active-address-pending-returns
  :<- [:district0x/active-address]
  :<- [:offering-registry/offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (get-in offerings [offering-address :auction-offering/pending-returns active-address])))

(reg-sub
  :auction-offering/active-address-winning-bidder?
  :<- [:district0x/active-address]
  :<- [:offering-registry/offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (= active-address (get-in offerings [offering-address :auction-offering/winning-bidder]))))

(reg-sub
  :offering/active-address-original-owner?
  :<- [:district0x/active-address]
  :<- [:offering-registry/offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (= active-address (get-in offerings [offering-address :offering/original-owner]))))

(reg-sub
  :offering/registrar-entry
  :<- [:offering-registry/offerings]
  :<- [:registrar/entries]
  (fn [[offerings registrar-entries] [_ offering-address]]
    (get registrar-entries (get-in offerings [offering-address :offering/label-hash]))))

(reg-sub
  :offering/ens-record
  :<- [:offering-registry/offerings]
  :<- [:ens/records]
  (fn [[offerings ens-records] [_ offering-address]]
    (get ens-records (get-in offerings [offering-address :offering/node]))))

(reg-sub
  :buy-now-offering/buy-tx-pending?
  (fn [[_ offering-address]]
    [(subscribe [:district0x/tx-pending? :buy-now-offering :buy {:offering/address offering-address}])])
  first)

(reg-sub
  :auction-offering/bid-tx-pending?
  (fn [[_ offering-address]]
    [(subscribe [:district0x/tx-pending? :auction-offering :bid {:offering/address offering-address}])])
  first)

(reg-sub
  :auction-offering/withdraw-tx-pending?
  (fn [[_ offering-address]]
    [(subscribe [:district0x/tx-pending? :auction-offering :withdraw {:offering/address offering-address}])])
  first)

(reg-sub
  :registrar/transfer-tx-pending?
  (fn [[_ ens-record-label]]
    [(subscribe [:district0x/tx-pending? :mock-registrar #_ :registrar :transfer {:ens.record/label ens-record-label}])])
  first)

(reg-sub
  :ens/set-owner-tx-pending?
  (fn [[_ ens-record-node]]
    [(subscribe [:district0x/tx-pending? :ens :set-owner {:ens.record/node ens-record-node}])])
  first)

(reg-sub
  :offering/reclaim-ownership-tx-pending?
  (fn [[_ offering-address]]
    (subscribe [:district0x/tx-pending? :buy-now-offering :reclaim-ownership {:offering/address offering-address}]))
  identity)

(reg-sub
  :offering/node-owner?
  (fn [[_ offering-address]]
    [(subscribe [:offering/ens-record offering-address])
     (subscribe [:offering/registrar-entry offering-address])])
  (fn [[ens-record registrar-entry] [_ offering-address]]
    (when (and (:ens.record/owner ens-record)
               (:registrar.entry.deed/owner registrar-entry))
      (= offering-address
         (:ens.record/owner ens-record)
         (:registrar.entry.deed/owner registrar-entry)))))

(reg-sub
  :offering/show-missing-ownership-warning?
  (fn [[_ offering-address]]
    [(subscribe [:offering-registry/offering offering-address])
     (subscribe [:offering/node-owner? offering-address])])
  (fn [[{:keys [:offering/new-owner]} node-owner?] [_ offering-address]]
    (and (false? node-owner?) (empty-address? new-owner))))

(reg-sub
  :offering/status
  (fn [[_ offering-address]]
    [(subscribe [:offering-registry/offering offering-address])
     (subscribe [:offering/node-owner? offering-address])
     (subscribe [:now])])
  (fn [[{:keys [:offering/new-owner :auction-offering/end-time :offering/type]} node-owner? now]]
    (cond
      new-owner :offering.status/finalized
      (not node-owner?) :offering.status/missing-ownership
      (and (= type :auction-offering) (t/after? now end-time)) :offering.status/finished
      :else :offering.status/active)))

(reg-sub
  :search-results/offerings
  (fn [[_ search-results-key]]
    [(subscribe [:district0x/search-results search-results-key])
     (subscribe [:offering-registry/offerings])])
  (fn [[search-results offerings]]
    (assoc search-results :items (map offerings (:ids search-results)))))

(reg-sub
  :search-results/home-page-autocomplete
  :<- [:search-results/offerings :search-results/home-page-autocomplete]
  identity)

(reg-sub
  :search-results/offerings-main-search
  :<- [:search-results/offerings :search-results/offerings-main-search]
  identity)

(reg-sub
  :search-params/offerings-main-search
  :<- [:search-results/offerings-main-search]
  :params)

(reg-sub
  :infinite-list
  (fn [db]
    (:infinite-list db)))

(reg-sub
  :infinite-list/expanded-items
  :<- [:infinite-list]
  (fn [infinite-list]
    (:expanded-items infinite-list)))

(reg-sub
  :infinite-list/item-expanded?
  :<- [:infinite-list/expanded-items]
  (fn [expanded-items [_ index]]
    (boolean (get expanded-items index))))

(reg-sub
  :infinite-list/expanded-item-body-height
  :<- [:infinite-list/expanded-items]
  (fn [expanded-items [_ index collapsed-height]]
    (if-let [item-height (get-in expanded-items [index :height])]
      (- item-height collapsed-height)
      0)))

(reg-sub
  :infinite-list/items-heights
  :<- [:infinite-list/expanded-items]
  (fn [expanded-items [_ items-count default-height]]
    (map #(get-in expanded-items [% :height] default-height) (range items-count))))
