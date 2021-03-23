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
  :eth-registrar/registrations
  (fn [db]
    (:eth-registrar/registrations db)))

(reg-sub
  :eth-registrar/registration
  :<- [:eth-registrar/registrations]
  (fn [registrations [_ label-hash]]
    (get registrations label-hash)))

(reg-sub
  :eth-registrar.registration/loaded?
  (fn [[_ label-hash]]
    (subscribe [:eth-registrar/registration label-hash]))
  registrar-registration-loaded?)

(reg-sub
  :eth-registrar.registration/active-address-owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/active-address])
     (subscribe [:eth-registrar/registration label-hash])])
  (fn [[active-address registrar-registration]]
    (and active-address (= active-address (:eth-registrar.registration/owner registrar-registration)))))

(reg-sub
  :eth-registrar.registration/my-addresses-contain-owner?
  (fn [[_ label-hash]]
    [(subscribe [:district0x/my-addresses])
     (subscribe [:eth-registrar/registration label-hash])])
  (fn [[my-addresses registrar-registration]]
    (contains? (set my-addresses) (:eth-registrar.registration/owner registrar-registration))))

(reg-sub
  :eth-registrar.transfer/tx-pending?
  (fn [[_ ens-record-label]]
    [(subscribe [:district0x/tx-pending? :eth-registrar :transfer {:ens.record/label ens-record-label}])])
  first)

(reg-sub
  :eth-registrar/expiration-date
  (fn [[_ label-hash]]
    [(subscribe [:eth-registrar/registration label-hash])])
  (fn [[{:keys [:eth-registrar.registration/expiration-date]}]]
    expiration-date))

(reg-sub
  :eth-registrar/ownership-time-remaining
  (fn [[_ label-hash]]
    [(subscribe [:eth-registrar/expiration-date label-hash])
     (subscribe [:now])])
  (fn [[end-ownership-date now]]
    (d0x-ui-utils/time-remaining now end-ownership-date)))

(reg-sub
  :eth-registrar.transact/tx-pending?
  (fn [[_ method label]]
    [(subscribe [:district0x/tx-pending? :eth-registrar method {:eth-registrar/label label}])])
  first)
