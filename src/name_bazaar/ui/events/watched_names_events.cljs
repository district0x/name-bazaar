(ns name-bazaar.ui.events.watched-names-events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [parse-offering-request]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.db :refer [default-db]]
    [name-bazaar.ui.utils :refer [namehash sha3 valid-ens-name? normalize get-ens-record-active-offering]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(reg-event-fx
  :watched-names/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [name]]
    (when (and (seq name)
               (valid-ens-name? name))
      (let [name (normalize name)
            node (namehash name)
            new-db (-> db
                     (update-in [:watched-names :order] conj node)
                     (assoc-in [:watched-names :ens/records node] {:ens.record/name name}))]
        (when-not (get-in db [:watched-names :ens/records node])
          {:db new-db
           :localstorage (merge localstorage (select-keys new-db [:watched-names]))
           :dispatch [:watched-names/load [node]]})))))

(reg-event-fx
  :watched-names/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [node]]
    (let [new-db (-> db
                   (update-in [:watched-names :order] (comp vec (partial remove #(= node %))))
                   (update-in [:watched-names :ens/records] dissoc node))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:watched-names]))})))

(reg-event-fx
  :watched-names/remove-all
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]}]
    (let [new-db (assoc db :watched-names (:watched-names default-db))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:watched-names]))})))

(reg-event-fx
  :watched-names/load-all
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db]} [nodes]]
    {:dispatch [:watched-names/load (:order (:watched-names db))]}))

(reg-event-fx
  :watched-names/load
  interceptors
  (fn [{:keys [:db]} [nodes]]
    (when (seq nodes)
      {:async-flow {:first-dispatch [:ens.records.active-offerings/load nodes]
                    :rules [{:when :seen?
                             :events [:ens.records.active-offerings/loaded]
                             :dispatch-n [[:watched-names.node-active-offerings/loaded nodes]]}]}})))

(defn- get-items-to-load [ens-records]
  (reduce (fn [acc [node {:keys [:ens.record/active-offering]}]]
            (if active-offering
              (update acc :offerings-to-load conj active-offering)
              (update acc :offering-requests-to-load conj node)))
          {:offerings-to-load []
           :offering-requests-to-load []}
          ens-records))

(reg-event-fx
  :watched-names.node-active-offerings/loaded
  interceptors
  (fn [{:keys [:db]} [nodes]]
    (let [{:keys [:offerings-to-load :offering-requests-to-load]}
          (get-items-to-load (select-keys (:ens/records db) nodes))]
      {:dispatch-n [[:offerings/load offerings-to-load]
                    [:offering-requests/load offering-requests-to-load]]})))