(ns name-bazaar.ui.subs.registrar-subs
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [district0x.ui.utils :as d0x-ui-utils]
    [medley.core :as medley]
    [name-bazaar.shared.constants :as constants]
    [name-bazaar.ui.utils :refer [registrar-entry-deed-loaded? seal-bid]]
    [re-frame.core :refer [reg-sub subscribe]]))

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
  (fn [[_ label-hash]]
    (subscribe [:registrar/entry label-hash]))
  registrar-entry-deed-loaded?)

(reg-sub
  :registrar.entry.deed/active-address-owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/active-address])
     (subscribe [:registrar/entry label-hash])])
  (fn [[active-address registrar-entry]]
    (and active-address (= active-address (:registrar.entry.deed/owner registrar-entry)))))

(reg-sub
  :registrar.entry.deed/my-addresses-contain-owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/my-addresses])
     (subscribe [:registrar/entry label-hash])])
  (fn [[my-addresses registrar-entry]]
    (contains? (set my-addresses) (:registrar.entry.deed/address registrar-entry))))

(reg-sub
  :registrar.transfer/tx-pending?
  (fn [[_ ens-record-label]]
    [(subscribe [:district0x/tx-pending? :registrar :transfer {:ens.record/label ens-record-label}])])
  first)

(defn query-end-bidding-date
  [registration-date {:keys [:minutes :hours] :as reveal-period}]
  (t/minus registration-date
           (cond
             minutes
             (t/minutes minutes)
             hours
             (t/hours hours))))

(reg-sub
  :registrar/end-bidding-date
  (fn [[_ label-hash]]
    [(subscribe [:registrar/entry label-hash])
     (subscribe [:district0x/config :reveal-period])])
  (fn [[{:keys [:registrar.entry/registration-date]}
        reveal-period]]
    (when registration-date
      (query-end-bidding-date registration-date reveal-period))))

(reg-sub
  :registrar/bidding-time-remaining
  (fn [[_ label-hash]]
    [(subscribe [:registrar/end-bidding-date label-hash])
     (subscribe [:now])])
  (fn [[end-bidding-date now]]
    (d0x-ui-utils/time-remaining now end-bidding-date)))

(reg-sub
  :registrar/reveal-time-remaining
  (fn [[_ label-hash]]
    [(subscribe [:registrar/entry label-hash])
     (subscribe [:now])])
  (fn [[{:keys [:registrar.entry/registration-date]} now]]
    (d0x-ui-utils/time-remaining now registration-date)))

(reg-sub
  :registrar/end-ownership-date
  (fn [[_ label-hash]]
    [(subscribe [:registrar/entry label-hash])])
  (fn [[{:keys [:registrar.entry/registration-date]}]]
    (t/plus registration-date (t/years (:years constants/ownership-period)))))

(reg-sub
  :registrar/ownership-time-remaining
  (fn [[_ label-hash]]
    [(subscribe [:registrar/end-ownership-date label-hash])
     (subscribe [:now])])
  (fn [[end-ownership-date now]]
    (d0x-ui-utils/time-remaining now end-ownership-date)))

(reg-sub
  :registrar.transact/tx-pending?
  (fn [[_ method label]]
    [(subscribe [:district0x/tx-pending? :registrar method {:registrar/label label}])])
  first)
