(ns name-bazaar.events
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
    [district0x.big-number :as bn]
    [district0x.debounce-fx]
    [district0x.events :refer [get-contract get-instance reg-empty-event-fx]]
    [district0x.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.constants :as constants]
    [name-bazaar.shared.utils :refer [parse-offering parse-english-auction-offering parse-ens-record parse-offering-requests-counts]]
    [name-bazaar.utils :refer [namehash sha3]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v]]))

(def check-spec-interceptor (after (partial district0x.events/check-and-throw :name-bazaar.db/db)))
(def interceptors [trim-v check-spec-interceptor])

(defn- node-name [db node]
  (get-in db [:ens/records node :ens.record/name]))

(reg-event-fx
  :instant-buy-offering-factory/create-offering
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-data form-data
                 :form-key :instant-buy-offering-factory/create-offering}]}))

(reg-event-fx
  :english-auction-offering-factory/create-offering
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-data form-data
                 :form-key :english-auction-offering-factory/create-offering}]}))

(reg-event-fx
  :instant-buy-offering/buy
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :instant-buy-offering/buy
                 :form-id (select-keys form-data [:contract-address])}]}))

(reg-event-fx
  :instant-buy-offering/set-settings
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :instant-buy-offering/set-settings
                 :form-id (select-keys form-data [:contract-address])}]}))

(reg-event-fx
  :english-auction-offering/bid
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :english-auction-offering/bid
                 :form-id (select-keys form-data [:contract-address])}]}))

(reg-event-fx
  :english-auction-offering/finalize
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :english-auction-offering/finalize
                 :form-id (select-keys form-data [:contract-address])}]}))

(reg-event-fx
  :english-auction-offering/withdraw
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :english-auction-offering/withdraw
                 :form-id (select-keys form-data [:contract-address])}]}))

(reg-event-fx
  :english-auction-offering/set-settings
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :english-auction-offering/set-settings
                 :form-id (select-keys form-data [:contract-address])}]}))


(reg-event-fx
  :offering/reclaim-ownership
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :offering/reclaim-ownership
                 :contract-key :instant-buy-offering
                 :form-id (select-keys form-data [:contract-address])}]}))

(reg-event-fx
  :ens/set-owner
  interceptors
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (cond-> form-data
                      (:ens.record/name form-data) (assoc :ens.record/node (namehash (:ens.record/name form-data))))]
      {:dispatch [:district0x.form/submit
                  {:form-data form-data
                   :form-key :ens/set-owner
                   :form-id (select-keys form-data [:ens.record/node])}]})))

(reg-event-fx
  :offering-requests/add-request
  interceptors
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x.form/submit
                {:form-key :offering-requests/add-request
                 :contract-key :offering-requests
                 :form-id (select-keys form-data [:offering-request/name])}]}))

(reg-event-fx
  :load-offering-ids
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.list/http-load
                (merge
                  {:list-key :list/offerings
                   :path "/offerings"
                   :on-success [:load-offerings]}
                  opts)]}))


(defn- english-auction-offering? [db offering-address]
  (= (get-in db [:offering-registry/offerings % :offering/type]) :english-auction-offering))

(reg-event-fx
  :load-offerings
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    (let [offering-abi (:abi (get-contract db :instant-buy-offering))]
      {:web3-fx.contract/constant-fns
       {:fns (for [offering-address offering-addresses]
               {:instance (web3-eth/contract-at (:web3 db) offering-abi offering-address)
                :method :offering
                :on-success [:offering-loaded {:offering/address offering-address
                                               :load-type-specific-data? true}]
                :on-error [:district0x.log/error]})}})))

(reg-event-fx
  :offering-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/address :load-type-specific-offerings?]} offering]]
    (let [offering (parse-offering address offering)]
      (merge {:db (-> db
                    (update-in [:offering-registry/offerings address] merge offering)
                    (update-in [:ens/records address] merge {:ens.record/node (:offering/node offering)
                                                             :ens.record/name (:offering/name offering)}))
              :dispatch [:load-ens-records [(:offering/node offering)]]
              (when (and :load-type-specific-offerings?
                         (= (:offering/type offering) :english-auction-offering))
                {:dispatch-n [[:load-english-auction-offerings]]})}))))

(reg-event-fx
  :load-english-auction-offerings
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    (let [english-auction-offering-abi (:abi (get-contract db :english-auction-offering))]
      {:web3-fx.contract/constant-fns
       {:fns (for [offering-address offering-addresses]
               (let [instance (web3-eth/contract-at (:web3 db) english-auction-offering-abi offering-address)]
                 {:instance instance
                  :method :english-auction-offering
                  :on-success [:english-auction-offering-loaded {:offering/address offering-address}]
                  :on-error [:district0x.log/error]}))}})))

(reg-event-fx
  :english-auction-offering-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/address]} english-auction-offering]]
    (let [offering (parse-english-auction-offering english-auction-offering)]
      {:db (update-in db [:offering-registry/offerings address] merge offering)})))

(reg-event-fx
  :load-ens-records
  interceptors
  (fn [{:keys [:db]} [ens-record-nodes]]
    {:web3-fx.contract/constant-fns
     {:fns (for [node ens-record-nodes]
             {:instance (get-contract db :ens)
              :method :english-auction-offering
              :on-success [:ens-record-loaded {:ens.record/node node}]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :ens-record-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens.record/node]} ens-record]]
    (let [ens-record (parse-ens-record node ens-record)]
      {:db (update-in db [:ens/records node] merge ens-record)})))

(reg-event-fx
  :load-offering-requests-ids
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.list/http-load
                (merge
                  {:list-key :list/offering-requests
                   :path "/offering-requests"
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


