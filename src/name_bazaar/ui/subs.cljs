(ns name-bazaar.ui.subs
  (:require
    [cemerick.url :as url]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [district0x.shared.utils :as d0x-shared-utils :refer [empty-address? zero-address?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [time-remaining time-biggest-unit format-time-duration-unit]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [emergency-state-new-owner]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.subs.ens-subs]
    [name-bazaar.ui.subs.infinite-list-subs]
    [name-bazaar.ui.subs.offering-requests-subs]
    [name-bazaar.ui.subs.offerings-subs]
    [name-bazaar.ui.subs.registrar-subs]
    [name-bazaar.ui.utils :refer [parse-query-params]]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :now
  (fn [db]
    (:now db)))

(reg-sub
  :saved-searches
  (fn [db [_ saved-searches-key]]
    (get-in db [:saved-searches saved-searches-key])))

(reg-sub
  :saved-search/active?
  (fn [[_ saved-searches-key]]
    [(subscribe [:saved-searches saved-searches-key])
     (subscribe [:district0x/query-string])])
  (fn [[saved-searches query-string]]
    (boolean (get saved-searches query-string))))

#_(reg-sub
    :search-form/watched-names
    :<- [:district0x/db :search-form/watched-names]
    :<- [:ens/records]
    :<- [:offerings]
    (fn [watched-ens-records ens-records offerings]
      (map (fn [ens-record]
             (-> ens-record
               (merge (ens-records (:ens.record/node ens-record)))
               (update :ens.record/last-offering offerings)))
           watched-ens-records)))

(reg-sub
  :search-results
  (fn [db [_ db-path]]
    (get-in db (concat [:search-results] db-path))))
