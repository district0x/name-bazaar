(ns name-bazaar.ui.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs-web3.utils :as web3-utils]
    [cljs.spec.alpha :as s]
    [clojure.data :as data]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.string :as string]
    [day8.re-frame.async-flow-fx]
    [district0x.shared.big-number :as bn]
    [district0x.ui.debounce-fx]
    [district0x.ui.events :refer [get-contract get-instance reg-empty-event-fx get-form-data]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [parse-offering parse-auction-offering parse-ens-record parse-offering-requests-counts]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.spec]
    [name-bazaar.ui.utils :refer [namehash sha3]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(def interceptors [trim-v (validate-db :name-bazaar.ui.db/db)])

(defn- node-name [db node]
  (get-in db [:ens/records node :ens.record/name]))

(reg-event-fx
  :buy-now-offering-factory/create-offering
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-data form-data
                 :form-key :form.buy-now-offering-factory/create-offering}]}))

(reg-event-fx
  :auction-offering-factory/create-offering
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-data form-data
                 :form-key :form.auction-offering-factory/create-offering}]}))

(reg-event-fx
  :buy-now-offering/buy
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.buy-now-offering/buy}]}))

(reg-event-fx
  :buy-now-offering/set-settings
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.buy-now-offering/set-settings}]}))

(reg-event-fx
  :auction-offering/bid
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.auction-offering/bid}]}))

(reg-event-fx
  :auction-offering/finalize
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.auction-offering/finalize}]}))

(reg-event-fx
  :auction-offering/withdraw
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.auction-offering/withdraw}]}))

(reg-event-fx
  :auction-offering/set-settings
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.auction-offering/set-settings}]}))


(reg-event-fx
  :offering/reclaim-ownership
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.offering/reclaim-ownership}]}))

(reg-event-fx
  :ens/set-owner
  interceptors
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (cond-> form-data
                      (:ens.record/name form-data) (assoc :ens.record/node (namehash (:ens.record/name form-data))))]
      {:dispatch [:district0x.form/submit
                  {:form-data form-data
                   :form-key :form.ens/set-owner}]})))

(reg-event-fx
  :offering-requests/add-request
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :form.offering-requests/add-request}]}))

(reg-event-fx
  :search/offerings
  interceptors
  (fn [{:keys [:db]} [opts load-opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:search-results-key :search-results/offerings
                   :endpoint "/offerings"
                   :on-success [:search/offerings-loaded load-opts]}
                  opts)]}))

(reg-event-fx
  :search/offerings-loaded
  interceptors
  (fn [{:keys [:db]} [load-opts offering-addresses]]
    {:dispatch [:load-offerings offering-addresses load-opts]}))

(reg-event-fx
  :search/home-page-search
  interceptors
  (fn [{:keys [:db]} [params]]
    {:dispatch-debounce {:key :search/home-page-search
                         :event [:search/offerings
                                 {:search-params (merge (get-form-data db :search-form/home-page-search)
                                                        params)}]
                         :delay 300}}))

(defn- auction-offering? [db offering-address]
  (= (get-in db [:offering-registry/offerings offering-address :offering/type]) :auction-offering))

(reg-event-fx
  :load-offerings
  interceptors
  (fn [{:keys [:db]} [offering-addresses {:keys [:load-type-specific-data?]}]]
    (let [offering-abi (:abi (get-contract db :buy-now-offering))]
      {:web3-fx.contract/constant-fns
       {:fns (for [offering-address offering-addresses]
               {:instance (web3-eth/contract-at (:web3 db) offering-abi offering-address)
                :method :offering
                :on-success [:offering-loaded {:offering/address offering-address
                                               :load-type-specific-data? load-type-specific-data?}]
                :on-error [:district0x.log/error]})}})))

(reg-event-fx
  :offering-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/address :load-type-specific-offerings?]} offering]]
    (let [{:keys [:offering/node] :as offering} (parse-offering address offering {:parse-dates? true})]
      (merge {:db (-> db
                    (update-in [:offering-registry/offerings address] merge offering)
                    (update-in [:ens/records node] merge {:ens.record/node node
                                                          :ens.record/name node}))}
             (when (and :load-type-specific-offerings?
                        (= (:offering/type offering) :auction-offering))
               {:dispatch-n [[:load-auction-offerings]]})))))

(reg-event-fx
  :load-auction-offerings
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    (let [auction-offering-abi (:abi (get-contract db :auction-offering))]
      {:web3-fx.contract/constant-fns
       {:fns (for [offering-address offering-addresses]
               (let [instance (web3-eth/contract-at (:web3 db) auction-offering-abi offering-address)]
                 {:instance instance
                  :method :auction-offering
                  :on-success [:auction-offering-loaded {:offering/address offering-address}]
                  :on-error [:district0x.log/error]}))}})))

(reg-event-fx
  :auction-offering-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/address]} auction-offering]]
    (let [offering (parse-auction-offering auction-offering {:parse-dates? true})]
      {:db (update-in db [:offering-registry/offerings address] merge offering)})))

(reg-event-fx
  :load-ens-records
  interceptors
  (fn [{:keys [:db]} [nodes]]
    {:web3-fx.contract/constant-fns
     {:fns (for [node nodes]
             {:instance (get-contract db :ens)
              :method :records
              :on-success [:ens-record-loaded {:ens.record/node node}]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :ens-record-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens.record/node]} ens-record]]
    (let [ens-record (parse-ens-record node ens-record {:parse-dates? true})]
      {:db (update-in db [:ens/records node] merge ens-record)})))

(reg-event-fx
  :search/ens-record-offerings
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens.record/node :ens.record/name]}]]
    (let [node (if name (namehash name) node)]
      {:dispatch [:search/offerings {:params {:node node
                                              :order-by-columns [:created-on]
                                              :order-by-dirs [:desc]}}]})))

(reg-event-fx
  :search/offering-requests
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:search-results-key :search-results/offering-requests
                   :endpoint "/offering-requests"
                   :on-success [:load-offering-requests]}
                  opts)]}))

(reg-event-fx
  :load-offering-requests
  interceptors
  (fn [{:keys [:db]} [nodes]]
    (let [[nodes-with-known-names nodes-with-unknown-names] (split-with (partial node-name db) nodes)]
      {:web3-fx.contract/constant-fns
       {:fns
        (remove nil?
                [(when (seq nodes-with-known-names)
                   {:instance (get-contract db :offering-requests)
                    :method :get-requests-counts
                    :args [nodes-with-known-names]
                    :on-success [:offering-requests-counts-loaded nodes-with-known-names]
                    :on-error [:district0x.log/error]})
                 (when (seq nodes-with-unknown-names)
                   {:instance (get-contract db :offering-requests)
                    :method :get-requests
                    :args [nodes-with-unknown-names]
                    :on-success [:offering-requests-loaded nodes-with-unknown-names]
                    :on-error [:district0x.log/error]})])}})))

(reg-event-fx
  :offering-requests-counts-loaded
  interceptors
  (fn [{:keys [:db]} [nodes counts]]
    (let [counts (map (comp (partial hash-map :offering-request/requesters-count) bn/->number) counts)]
      {:db (update-in db :offering-requests/requests
                      (partial merge-with merge)
                      (parse-offering-requests-counts nodes counts))})))

(reg-event-fx
  :offering-requests-loaded
  interceptors
  (fn [{:keys [:db]} [nodes [names counts]]]
    {:db (update db :ens/records
                 (partial merge-with merge)
                 (zipmap nodes (map (partial hash-map :ens.record/name) names)))
     :dispatch [:offering-requests-counts-loaded nodes counts]}))

(reg-event-fx
  :load-offering-requests-has-requested
  interceptors
  (fn [{:keys [:db]} [node addresses]]
    (let [addresses (if-not addresses (:my-addresses db) addresses)]
      {:web3-fx.contract/constant-fns
       {:fns [{:instance (get-contract db :offering-requests)
               :method :has-requested
               :args [node addresses]
               :on-success [:offering-requests-has-requested-loaded node addresses]
               :on-error [:district0x.log/error]}]}})))

(reg-event-fx
  :offering-requests-has-requested-loaded
  interceptors
  (fn [{:keys [:db]} [node addresses has-requested-vals]]
    (let [[addrs-requested addrs-not-requested] (->> (zipmap addresses has-requested-vals)
                                                  (split-with second)
                                                  (map (partial map first)))]
      {:db (update-in db [:offering-requests/requests node :offering-request/requesters]
                      (fn [requesters]
                        (-> (set requesters)
                          (set/difference addrs-not-requested)
                          (set/union addrs-requested))))})))

(reg-event-fx
  :ens-records-last-offering-loaded
  interceptors
  (fn [{:keys [:db]} [results]]
    (update db :ens/records merge #(hash-map (:node %) {:ens.record/last-offering (:last-offering %)}))))

(reg-event-fx
  :search/watched-names
  interceptors
  (fn [{:keys [:db]}]
    (let [nodes (map :ens.record/node (get-form-data db :search-form/watched-names))]
      {:dispatch-n [[:load-nodes-last-offering-ids nodes]
                    [:load-ens-records nodes]]})))

(reg-event-fx
  :search-form.watched-names/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [name]]
    (let [node (namehash name)
          new-db (update-in db [:search-form/watched-names :data :watched-names/ens-records]
                            conj
                            {:ens.record/name name :ens.record/node node})]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db :search-form/watched-names))
       :dispatch-n [[:load-nodes-last-offering-ids [node]]
                    [:load-ens-records [node]]]})))

(reg-event-fx
  :search-form.watched-names/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [node]]
    (let [new-db (update db
                         [:search-form/watched-names :data :watched-names/ens-records]
                         (partial remove #(= node (:ens.record/node %))))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db :search-form/watched-names))})))

(reg-event-fx
  :watch-on-offering-added
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens.record/node :ens.record/owner :on-success :on-error]}]]
    {:dispatch [:district0x.contract/event-watch-once {:contract-key :offering-registry
                                                       :event-name :on-offering-added
                                                       :event-filter-opts {:node node :owner owner}
                                                       :blockchain-filter-opts "latest"
                                                       :on-success [:offering-registry/on-offering-added]}]}))

(reg-event-fx
  :offering-registry/on-offering-added
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering :owner :node]} event]]
    {:db (-> db
           (update-in [:offering-registry/offerings offering] merge {:offering/node node
                                                                     :offering/original-owner owner})
           (update-in [:ens/records node] (fn [ens-node]
                                            (merge ens-node
                                                   {:ens.record/owner owner
                                                    :node/offerings (conj (vec (:node/offerings ens-node))
                                                                          offering)}))))}))

(reg-event-fx
  :transfer-node-owner-to-latest-offering
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens.record/name]}]]
    (let [node (namehash name)
          offering (last (get-in db [:ens/records node :node/offerings]))]
      {:dispatch [:ens/set-owner {:ens.record/node node :ens.record/owner offering}]})))


