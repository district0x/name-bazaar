(ns name-bazaar.ui.subs.offering-requests-subs
  (:require
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :offering-requests
  (fn [db]
    (:offering-requests db)))

(reg-sub
  :offering-request
  :<- [:offering-requests]
  (fn [offering-requests [_ node]]
    (get offering-requests node)))

(reg-sub
  :offering-requests/search-results
  (fn [[_ search-results-key]]
    [(subscribe [:search-results [:offering-requests search-results-key]])
     (subscribe [:offering-requests])])
  (fn [[search-results offering-requests]]
    (assoc search-results :items (map offering-requests (:ids search-results)))))

(reg-sub
  :offering-requests/main-search
  :<- [:offering-requests/search-results :main-search]
  identity)

(reg-sub
  :offering-requests.main-search/params
  :<- [:offering-requests/main-search]
  :params)

(reg-sub
  :offering-request/active-address-has-requested?
  (fn [[_ node]]
    [(subscribe [:offering-request node])
     (subscribe [:district0x/active-address])])
  (fn [[{:keys [:offering-request/requesters]} active-address]]
    (and active-address (contains? requesters active-address))))

(reg-sub
  :offering-requests.add-request/tx-pending?
  (fn [[_ ens-record-name]]
    (subscribe [:district0x/tx-pending? :offering-requests :add-request {:ens.record/name ens-record-name}]))
  identity)

