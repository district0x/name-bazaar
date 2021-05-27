(ns name-bazaar.ui.subs.watched-names-subs
  (:require
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :watched-names
  (fn [db]
    (:watched-names db)))

(reg-sub
  :watched-names/watched-items
  :<- [:watched-names]
  :<- [:ens/records]
  :<- [:offerings]
  (fn [[watched-names ens-records offerings]]
    (map (fn [node]
           (let [{:keys [:ens.record/name] :as watched-ens-record} (get-in watched-names [:ens/records node])]
             (merge
               watched-ens-record
               {:ens.record/node node}
               (if-let [offering-address (get-in ens-records [node :ens.record/active-offering])]
                 (get offerings offering-address)))))
         (:order watched-names))))

(reg-sub
  :watched-names.node/watched?
  :<- [:watched-names]
  (fn [watched-names [_ node]]
    (boolean (get-in watched-names [:ens/records node]))))

