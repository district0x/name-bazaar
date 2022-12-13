(ns name-bazaar.ui.subs.ens-subs
  (:require
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [top-level-name? name-label normalize]]
    [name-bazaar.ui.utils :refer [namehash sha3 registrar-registration-loaded? ens-record-loaded?]]
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
    (and active-address (= active-address (:ens.record/owner ens-record)))))

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


(reg-sub
  :ens.record/ownership-status
  (fn [[_ name]]
    (let [node (namehash name)
          label-hash (sha3 (name-label name))]
      [(subscribe [:ens.record/loaded? node])
       (subscribe [:eth-registrar.registration/loaded? label-hash])
       (subscribe [:ens.record/active-address-owner? node])
       (subscribe [:eth-registrar.registration/active-address-owner? label-hash])]))
  (fn [[ens-record-loaded? registration-loaded? active-address-ens-owner? active-address-registration-owner?] [_ name]]
    (cond
      (empty? name)
      :ens.ownership-status/empty-name

      (not (and ens-record-loaded?
                (if (top-level-name? name)
                  registration-loaded?
                  true)))
      :ens.ownership-status/loading

      (not active-address-ens-owner?)
      :ens.ownership-status/not-ens-record-owner

      (and (top-level-name? name)
           (not active-address-registration-owner?))
      :ens.ownership-status/not-registration-owner

      :else :ens.ownership-status/owner)))

(reg-sub
  :ens.record/resolver
  :<- [:ens/records]
  (fn [records [_ node]]
    (get-in records [node :ens.record/resolver])))

(reg-sub
  :ens.record/default-resolver?
  (fn [db [_ node]]
    (= (normalize (get-in db [:smart-contracts :public-resolver :address]))
       (normalize @(subscribe [:ens.record/resolver node])))))

(reg-sub
  :ens.set-resolver/tx-pending?
  (fn [[_ ens-record-node]]
    [(subscribe [:district0x/tx-pending? :ens :set-resolver {:ens.record/node ens-record-node}])])
  first)

(reg-sub
  :ens.set-subnode-owner/tx-pending?
  (fn [[_ ens-record-node ens-record-label]]
    [(subscribe [:district0x/tx-pending? :ens :set-subnode-owner {:ens.record/node ens-record-node
                                                                  :ens.record/label ens-record-label}])])
  first)
