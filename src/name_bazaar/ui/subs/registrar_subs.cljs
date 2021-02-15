(ns name-bazaar.ui.subs.registrar-subs
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [district0x.ui.utils :as d0x-ui-utils]
    [medley.core :as medley]
    [name-bazaar.shared.constants :as constants]
    [name-bazaar.ui.utils :refer [registrar-registration-loaded? seal-bid]]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :name-bazaar-registrar/registrations
  (fn [db]
    (:name-bazaar-registrar/registrations db)))

(reg-sub
  :name-bazaar-registrar/registration
  :<- [:name-bazaar-registrar/registrations]
  (fn [registrations [_ label-hash]]
    (get registrations label-hash)))

(reg-sub
  :name-bazaar-registrar.registration/loaded?
  (fn [[_ label-hash]]
    (subscribe [:name-bazaar-registrar/registration label-hash]))
  registrar-registration-loaded?)

(reg-sub
  :name-bazaar-registrar.registration/active-address-owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/active-address])
     (subscribe [:name-bazaar-registrar/registration label-hash])])
  (fn [[active-address registrar-registration]]
    (and active-address (= active-address (:name-bazaar-registrar.registration/owner registrar-registration)))))

(reg-sub
  :name-bazaar-registrar.registration/my-addresses-contain-owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/my-addresses])
     (subscribe [:name-bazaar-registrar/registration label-hash])])
  (fn [[my-addresses registrar-registration]]
    (contains? (set my-addresses) (:name-bazaar-registrar.registration/owner registrar-registration))))

(reg-sub
  :name-bazaar-registrar.transfer/tx-pending?
  (fn [[_ ens-record-label]]
    [(subscribe [:district0x/tx-pending? :name-bazaar-registrar :transfer {:ens.record/label ens-record-label}])])
  first)

(reg-sub
  :name-bazaar-registrar/expiration-date
  (fn [[_ label-hash]]
    [(subscribe [:name-bazaar-registrar/registration label-hash])])
  (fn [[{:keys [:name-bazaar-registrar.registration/expiration-date]}]]
    expiration-date))

(reg-sub
  :name-bazaar-registrar/ownership-time-remaining
  (fn [[_ label-hash]]
    [(subscribe [:name-bazaar-registrar/expiration-date label-hash])
     (subscribe [:now])])
  (fn [[end-ownership-date now]]
    (d0x-ui-utils/time-remaining now end-ownership-date)))

(reg-sub
  :name-bazaar-registrar.transact/tx-pending?
  (fn [[_ method label]]
    [(subscribe [:district0x/tx-pending? :name-bazaar-registrar method {:name-bazaar-registrar/label label}])])
  first)
