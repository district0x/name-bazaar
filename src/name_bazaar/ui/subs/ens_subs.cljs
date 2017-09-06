(ns name-bazaar.ui.subs.ens-subs
  (:require
    [medley.core :as medley]
    [name-bazaar.ui.utils :refer [ens-record-loaded?]]
    [re-frame.core :refer [reg-sub subscribe]]))

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
  ens-record-loaded?)

(reg-sub
  :ens.record/active-address-owner?
  (fn [[_ node]]
    [(subscribe [:district0x/active-address])
     (subscribe [:ens/record node])])
  (fn [[active-address ens-record]]
    (= active-address (:ens.record/owner ens-record))))

(reg-sub
  :ens.record/my-addresses-contain-owner?
  (fn [[_ node]]
    [(subscribe [:district0x/my-addresses])
     (subscribe [:ens/record node])])
  (fn [[my-addresses ens-record]]
    (contains? (set my-addresses) (:ens.record/owner ens-record))))

(reg-sub
  :ens.set-owner/tx-pending?
  (fn [[_ ens-record-node]]
    [(subscribe [:district0x/tx-pending? :ens :set-owner {:ens.record/node ens-record-node}])])
  first)
