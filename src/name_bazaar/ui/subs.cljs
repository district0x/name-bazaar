(ns name-bazaar.ui.subs
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [district0x.ui.utils :refer [reg-form-sub]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [reg-sub]]))

(doseq [form-key (keys constants/form-configs)]
  (reg-form-sub
    form-key
    (fn [[form]]
      form)))

(reg-sub
  :offering-registry/offerings
  (fn [db]
    (:offering-registry/offerings db)))

(reg-sub
  :offering-requests/requests
  (fn [db]
    (:offering-requests/requests db)))

(reg-sub
  :ens/records
  (fn [db]
    (:ens/records db)))

(reg-sub
  :search-form/watched-names
  :<- [:district0x/db :search-form/watched-names]
  :<- [:ens/records]
  :<- [:offering-registry/offerings]
  (fn [watched-ens-records ens-records offerings]
    (map (fn [ens-record]
           (-> ens-record
             (merge (ens-records (:ens.record/node ens-record)))
             (update :ens.record/last-offering offerings)))
         watched-ens-records)))

(reg-sub
  :search-results/offerings
  :<- [:district0x/search-results :search-results/offerings]
  :<- [:offering-registry/offerings]
  (fn [[search-results offerings] [_ search-params]]
    (update search-results search-params #(assoc % :items (map offerings (:ids %))))))

(reg-sub
  :search-results/offering-requests
  :<- [:district0x/search-results :search-results/offering-requests]
  :<- [:offering-requests/requests]
  (fn [[search-results offering-requests] [_ search-params]]
    (update search-results search-params #(assoc % :items (map offering-requests (:ids %))))))