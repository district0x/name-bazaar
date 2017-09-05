(ns name-bazaar.ui.events.watched-names
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [parse-offering-requests-counts]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 parse-query-params path-for get-node-name get-offering-name get-offering auction-offering?]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(reg-event-fx
  :watched-names/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [name]]
    (let [node (namehash name)
          new-db (update-in db [:search-form/watched-names :data :watched-names/ens-records]
                            conj
                            {:ens.record/name name :ens.record/node node})]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db :search-form/watched-names))
       :dispatch-n [[:load-nodes-last-offering-ids [node]]
                    [:ens.records/load [node]]]})))

(reg-event-fx
  :watched-names/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [node]]
    (let [new-db (update db
                         [:search-form/watched-names :data :watched-names/ens-records]
                         (partial remove #(= node (:ens.record/node %))))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db :search-form/watched-names))})))