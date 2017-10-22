(ns name-bazaar.ui.subs.registrar-subs
  (:require
    [medley.core :as medley]
    [name-bazaar.ui.utils :refer [registrar-entry-deed-loaded?]]
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



