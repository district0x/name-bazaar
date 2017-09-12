(ns name-bazaar.ui.subs.offerings-subs
  (:require
    [cljs-time.core :as t]
    [district0x.shared.utils :as d0x-shared-utils :refer [empty-address? zero-address?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [time-remaining time-biggest-unit format-time-duration-unit]]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [emergency-state-new-owner]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [registrar-entry-deed-loaded? ens-record-loaded?]]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :offerings
  (fn [db]
    (:offerings db)))

(reg-sub
  :offering
  :<- [:offerings]
  (fn [offerings [_ offering-address]]
    (get offerings offering-address)))

(reg-sub
  :offerings.main-search.drawer/open?
  (fn [db]
    (get-in db [:offerings-main-search-drawer :open?])))

(reg-sub
  :offerings/route-offering
  :<- [:district0x/route-params]
  :<- [:offerings]
  (fn [[route-params offerings]]
    (get-in offerings [(:offering/address route-params)])))

(reg-sub
  :offering/loaded?
  (fn [[_ offering-address]]
    [(subscribe [:offering offering-address])
     (subscribe [:registrar/entries])
     (subscribe [:ens/records])])
  (fn [[{:keys [:offering/label-hash :offering/node :offering/name :auction-offering/end-time :offering/buy-now?
                :offering/top-level-name?]}
        registrar-entries ens-records]]
    (let [registrar-entry (get registrar-entries label-hash)
          ens-record (get ens-records node)]
      (and (seq name)
           (or buy-now?
               end-time)
           (or (and top-level-name?
                    (registrar-entry-deed-loaded? registrar-entry)
                    (ens-record-loaded? ens-record))
               (and (not top-level-name?)
                    (ens-record-loaded? ens-record)))))))

(reg-sub
  :auction-offering/end-time-countdown
  (fn [[_ offering-address]]
    [(subscribe [:offering offering-address])
     (subscribe [:now])])
  (fn [[offering now]]
    (time-remaining now (:auction-offering/end-time offering))))

(reg-sub
  :auction-offering/end-time-countdown-biggest-unit
  (fn [[_ offering-address]]
    (subscribe [:auction-offering/end-time-countdown offering-address]))
  (fn [time-remaining]
    (apply format-time-duration-unit (time-biggest-unit time-remaining))))

(reg-sub
  :auction-offering/end-time-ended?
  (fn [[_ offering-address]]
    [(subscribe [:offering offering-address])
     (subscribe [:now])])
  (fn [[{:keys [:auction-offering/end-time]} now]]
    (and end-time (t/after? now end-time))))

(reg-sub
  :auction-offering/active-address-pending-returns
  :<- [:district0x/active-address]
  :<- [:offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (get-in offerings [offering-address :auction-offering/pending-returns active-address])))

(reg-sub
  :auction-offering/active-address-winning-bidder?
  :<- [:district0x/active-address]
  :<- [:offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (= active-address (get-in offerings [offering-address :auction-offering/winning-bidder]))))

(reg-sub
  :offering/active-address-original-owner?
  :<- [:district0x/active-address]
  :<- [:offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (= active-address (get-in offerings [offering-address :offering/original-owner]))))

(reg-sub
  :offering/active-address-new-owner?
  :<- [:district0x/active-address]
  :<- [:offerings]
  (fn [[active-address offerings] [_ offering-address]]
    (= active-address (get-in offerings [offering-address :offering/new-owner]))))

(reg-sub
  :offering/registrar-entry
  :<- [:offerings]
  :<- [:registrar/entries]
  (fn [[offerings registrar-entries] [_ offering-address]]
    (get registrar-entries (get-in offerings [offering-address :offering/label-hash]))))

(reg-sub
  :offering/ens-record
  :<- [:offerings]
  :<- [:ens/records]
  (fn [[offerings ens-records] [_ offering-address]]
    (get ens-records (get-in offerings [offering-address :offering/node]))))

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
    [(subscribe [:offering offering-address])
     (subscribe [:offering/node-owner? offering-address])])
  (fn [[{:keys [:offering/new-owner]} node-owner?] [_ offering-address]]
    (and (false? node-owner?) (empty-address? new-owner))))

(reg-sub
  :offering/status
  (fn [[_ offering-address]]
    [(subscribe [:offering offering-address])
     (subscribe [:offering/node-owner? offering-address])
     (subscribe [:now])])
  (fn [[{:keys [:offering/new-owner :auction-offering/end-time :offering/auction?]} node-owner? now]]
    (cond
      (= new-owner emergency-state-new-owner) :offering.status/emergency
      (not (empty-address? new-owner)) :offering.status/finalized
      (not node-owner?) :offering.status/missing-ownership
      (and auction? (t/after? now end-time)) :offering.status/auction-ended
      :else :offering.status/active)))

(reg-sub
  :offering/active?
  (fn [[_ offering-address]]
    [(subscribe [:offering offering-address])
     (subscribe [:offering/node-owner? offering-address])
     (subscribe [:auction-offering/end-time-ended? offering-address])])
  (fn [[{:keys [:offering/buy-now?]} node-owner? end-time-ended?]]
    (and node-owner?
         (or buy-now?
             (not end-time-ended?)))))

(reg-sub
  :offerings/search-results
  (fn [[_ search-results-key]]
    [(subscribe [:search-results [:offerings search-results-key]])
     (subscribe [:offerings])])
  (fn [[search-results offerings]]
    (assoc search-results :items (map offerings (:ids search-results)))))

(reg-sub
  :offerings/saved-searches
  :<- [:saved-searches :offerings-search]
  identity)

(reg-sub
  :offerings.saved-search/active?
  :<- [:saved-search/active? :offerings-search]
  identity)

(reg-sub
  :offerings/home-page-autocomplete
  :<- [:offerings/search-results :home-page-autocomplete]
  identity)

(reg-sub
  :offerings/main-search
  :<- [:offerings/search-results :main-search]
  identity)

(reg-sub
  :offerings.main-search/params
  :<- [:offerings/main-search]
  :params)

(reg-sub
  :offerings/ens-record-offerings
  :<- [:offerings/search-results :ens-record-offerings]
  identity)

(reg-sub
  :offerings/similar-offerings
  :<- [:offerings/search-results :similar-offerings]
  identity)

(reg-sub
  :offerings/user-purchases
  :<- [:offerings/search-results :user-purchases]
  identity)

(reg-sub
  :buy-now-offering.buy/tx-pending?
  (fn [[_ offering-address]]
    (subscribe [:district0x/tx-pending? :buy-now-offering :buy {:offering/address offering-address}]))
  identity)

(reg-sub
  :auction-offering.bid/tx-pending?
  (fn [[_ offering-address]]
    (subscribe [:district0x/tx-pending? :auction-offering :bid {:offering/address offering-address}]))
  identity)

(reg-sub
  :auction-offering.withdraw/tx-pending?
  (fn [[_ offering-address]]
    (subscribe [:district0x/tx-pending? :auction-offering :withdraw {:offering/address offering-address}]))
  identity)

(reg-sub
  :offering.reclaim-ownership/tx-pending?
  (fn [[_ offering-address]]
    (subscribe [:district0x/tx-pending? :buy-now-offering :reclaim-ownership {:offering/address offering-address}]))
  identity)

(reg-sub
  :auction-offering.finalize/tx-pending?
  (fn [[_ offering-address]]
    (subscribe [:district0x/tx-pending? :auction-offering :finalize {:offering/address offering-address}]))
  identity)