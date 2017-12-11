(ns name-bazaar.ui.subs.public-resolver-subs
  (:require
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :public-resolver/records
  (fn [db]
    (:public-resolver/records db)))

(reg-sub
  :public-resolver/reverse-records
  (fn [db]
    (:public-resolver/reverse-records db)))

(reg-sub
 :public-resolver.set-addr/tx-pending?
 (fn [[_ ens-record-node]]
   [(subscribe [:district0x/tx-pending? :public-resolver :set-addr {:ens.record/node ens-record-node}])])
 first)

(reg-sub
 :public-resolver.set-name/tx-pending?
 (fn [[_ ens-record-node]]
   [(subscribe [:district0x/tx-pending? :public-resolver :set-name {:ens.record/node ens-record-node}])])
 first)
