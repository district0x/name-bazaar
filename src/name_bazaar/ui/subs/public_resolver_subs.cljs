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
 :public-resolver.point-name/tx-pending?
 (fn [[_ ens-record-node ens-record-addr]]
   [(subscribe [:district0x/tx-pending? :public-resolver :set-addr {:ens.record/node ens-record-node
                                                        :ens.record/address ens-record-addr}])])
 first)

(reg-sub
 :public-resolver.point-address/tx-pending?
 (fn [[_ ens-record-node ens-record-name]]
   [(subscribe [:district0x/tx-pending? :public-resolver :set-name {:ens.record/node ens-record-node
                                                                    :ens.record/name ens-record-name}])])
 first)
