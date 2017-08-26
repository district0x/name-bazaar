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
    [day8.re-frame.forward-events-fx]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.debounce-fx]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils :refer [url-query-params->form-data]]
    [name-bazaar.ui.db :refer [default-db]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [parse-offering parse-auction-offering parse-offering-requests-counts parse-registrar-entry]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price]]
    [name-bazaar.ui.spec]
    [name-bazaar.ui.utils :refer [namehash sha3 parse-query-params path-for]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(def interceptors [trim-v (validate-db :name-bazaar.ui.db/db)])

(defn- node-name [db node]
  (get-in db [:ens/records node :ens.record/name]))

(defn- offering-name [db offering-address]
  (get-in db [:offering-registry/offerings offering-address :offering/name]))

(defn- auction-offering? [db offering-address]
  (= (get-in db [:offering-registry/offerings offering-address :offering/type]) :auction-offering))

(reg-event-fx
  :active-page-changed
  interceptors
  (fn [{:keys [:db]}]
    (let [{:keys [:handler :query-params]} (:active-page db)
          dispatch-n
          (condp = handler
            :route.offerings/search [[:set-params-and-search-offerings-main-search
                                      (merge
                                        (get-in default-db [:search-results/offerings-main-search :params])
                                        (parse-query-params query-params :route.offerings/search))
                                      {:clear-existing-items? true
                                       :clear-existing-params? true}]]
            [])]
      {:dispatch-n dispatch-n
       :db (assoc-in db [:infinite-list :expanded-items] {})})))

(reg-event-fx
  :buy-now-offering-factory/create-offering
  [interceptors (validate-first-arg (s/keys :req [:offering/name :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :buy-now-offering
                 :contract-method :create-offering
                 :form-data form-data
                 :tx-opts {:gas 700000 :gas-price default-gas-price}
                 :args-order [:offering/name
                              :offering/price]}]}))

(reg-event-fx
  :auction-offering-factory/create-offering
  [interceptors (validate-first-arg (s/keys :req [:offering/name
                                                  :offering/price
                                                  :auction-offering/end-time
                                                  :auction-offering/extension-duration
                                                  :auction-offering/min-bid-increase]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :auction-offering-factory
                 :contract-method :create-offering
                 :form-data form-data
                 :tx-opts {:gas 700000 :gas-price default-gas-price}
                 :args-order [:offering/name
                              :offering/price
                              :auction-offering/end-time
                              :auction-offering/extension-duration
                              :auction-offering/min-bid-increase]
                 :wei-keys #{:offering/price :auction-offering/min-bid-increase}}]}))

(reg-event-fx
  :buy-now-offering/buy
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :buy-now-offering
                 :contract-method :buy
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :tx-opts {:gas 100000
                           :gas-price default-gas-price
                           :value (:offering/price form-data)}
                 :form-id (select-keys form-data [:offering/address])}]}))

(reg-event-fx
  :buy-now-offering/set-settings
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :buy-now-offering
                 :contract-method :set-settings
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :args-order [:offering/price]
                 :tx-opts {:gas 250000 :gas-price default-gas-price}
                 :form-id (select-keys form-data [:offering/address])
                 :wei-keys #{:offering/price}}]}))

(reg-event-fx
  :auction-offering/bid
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Bid for %s" (offering-name db (:offering/address form-data)))
                 :contract-key :auction-offering
                 :contract-method :bid
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :result-href (path-for :route.offering/detail form-data)
                 :tx-opts {:gas 100000
                           :gas-price default-gas-price
                           :value (d0x-shared-utils/eth->wei (:offering/price form-data))}
                 :wei-keys #{:offering/price}
                 :form-id (select-keys form-data [:offering/address])}]}))

(reg-event-fx
  :auction-offering/finalize
  [interceptors (validate-first-arg (s/keys :req [:offering/address :auction-offering/transfer-price?]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :auction-offering
                 :contract-method :finalize
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :args-order [:auction-offering/transfer-price?]
                 :tx-opts {:gas 300000
                           :gas-price default-gas-price
                           :value (:offering/price form-data)}
                 :form-id (select-keys form-data [:offering/address])}]}))

(reg-event-fx
  :auction-offering/withdraw
  [interceptors (validate-first-arg (s/keys :req [:offering/address] :opt [:auction-offering/bidder]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (if-not (:auction-offering/bidder form-data)
                      (assoc form-data :auction-offering/bidder (:active-address form-data))
                      form-data)]
      {:dispatch [:district0x/make-transaction
                  {:contract-key :auction-offering
                   :contract-method :withdraw
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :args-order [:auction-offering/bidder]
                   :tx-opts {:gas 70000 :gas-price default-gas-price}
                   :form-id (select-keys form-data [:offering/address])}]})))

(reg-event-fx
  :auction-offering/set-settings
  [interceptors (validate-first-arg (s/keys :req [:offering/price
                                                  :auction-offering/end-time
                                                  :auction-offering/extension-duration
                                                  :auction-offering/min-bid-increase]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :auction-offering
                 :contract-method :set-settings
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :args-order [:offering/price
                              :auction-offering/end-time
                              :auction-offering/extension-duration
                              :auction-offering/min-bid-increase]
                 :form-id (select-keys form-data [:offering/address])
                 :tx-opts {:gas 1000000 :gas-price default-gas-price}
                 :wei-keys #{:offering/price}}]}))


(reg-event-fx
  :offering/reclaim-ownership
  [interceptors (validate-first-arg (s/keys :req [:offering/address]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :buy-now-offering
                 :contract-method :reclaim-ownership
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :form-id (select-keys form-data [:offering/address])
                 :tx-opts {:gas 200000 :gas-price default-gas-price}}]}))

(reg-event-fx
  :ens/set-owner
  [interceptors (validate-first-arg (s/keys :req [:ens.record/owner]
                                            :opt [:ens.record/name
                                                  :ens.record/node]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (cond-> form-data
                      (:ens.record/name form-data) (assoc :ens.record/node (namehash (:ens.record/name form-data))))]
      {:dispatch [:district0x/make-transaction
                  {:contract-key :ens
                   :contract-method :set-owner
                   :form-data form-data
                   :args-order [:ens.record/node :ens.record/owner]
                   :form-id (select-keys form-data [:ens.record/node])
                   :tx-opts {:gas 100000 :gas-price default-gas-price}}]})))

(reg-event-fx
  :offering-requests/add-request
  [interceptors (validate-first-arg (s/keys :req [:ens.record/name]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:contract-key :offering-requests
                 :contract-method :add-request
                 :form-data form-data
                 :args-order [:ens.record/name]
                 :form-id (select-keys form-data [:ens.record/name])
                 :tx-opts {:gas 100000 :gas-price default-gas-price}}]}))

(reg-event-fx
  :mock-registrar/register
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (assoc form-data :ens.record/label-hash (sha3 (:ens.record/label form-data)))]
      {:dispatch [:district0x/make-transaction
                  {:contract-key :mock-registrar
                   :contract-method :register
                   :form-data form-data
                   :args-order [:ens.record/label-hash]
                   :tx-opts {:gas 700000 :gas-price default-gas-price}}]})))


(reg-event-fx
  :search-home-page-autocomplete
  interceptors
  (fn [{:keys [:db]} [search-params]]
    {:dispatch [:search-offerings {:search-results-key :search-results/home-page-autocomplete
                                   :params search-params}]}))

(reg-event-fx
  :search-offerings-main-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    {:dispatch [:search-offerings
                (merge
                  {:search-results-key :search-results/offerings-main-search
                   :append? true
                   :params (d0x-shared-utils/update-multi
                             search-params
                             [:min-price :max-price]
                             d0x-shared-utils/safe-eth->wei->num)}
                  opts)]}))

(reg-event-fx
  :set-params-and-search-offerings-main-search
  interceptors
  (fn [{:keys [:db]} [search-params {:keys [:add-to-query? :clear-existing-params?] :as search-opts}]]
    (if add-to-query?
      {:dispatch [:district0x.location/add-to-query search-params]}
      (let [new-db (if clear-existing-params?
                     (assoc-in db [:search-results/offerings-main-search :params] search-params)
                     (update-in db [:search-results/offerings-main-search :params] merge search-params))]
        {:db new-db
         :dispatch [:search-offerings-main-search
                    (get-in new-db [:search-results/offerings-main-search :params])
                    search-opts]}))))

(reg-event-fx
  :search-offerings
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:endpoint "/offerings"
                   :on-success [:load-offerings]}
                  opts)]}))

(reg-event-fx
  :load-offerings
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (for [offering-address offering-addresses]
             {:instance (get-instance db :buy-now-offering offering-address)
              :method :offering
              :on-success [:offering-loaded offering-address]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :offering-loaded
  interceptors
  (fn [{:keys [:db]} [offering-address offering]]
    (let [{:keys [:offering/node :offering/name :offering/label-hash] :as offering}
          (parse-offering offering-address offering {:parse-dates? true :convert-to-ether? true})]
      (merge {:db (-> db
                    (update-in [:offering-registry/offerings offering-address] merge offering)
                    (update-in [:ens/records node] merge {:ens.record/node node
                                                          :ens.record/name name
                                                          :ens.record/label-hash label-hash}))}
             (when (= (:offering/type offering) :auction-offering)
               {:dispatch-n [[:load-auction-offerings [offering-address]]]})))))

(reg-event-fx
  :load-auction-offerings
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (for [offering-address offering-addresses]
             {:instance (get-instance db :auction-offering offering-address)
              :method :auction-offering
              :on-success [:auction-offering-loaded offering-address]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :auction-offering-loaded
  interceptors
  (fn [{:keys [:db]} [offering-address auction-offering]]
    (let [offering (parse-auction-offering auction-offering {:parse-dates? true :convert-to-ether? true})]
      {:db (update-in db [:offering-registry/offerings offering-address] merge offering)})))

(reg-event-fx
  :load-my-addresses-auction-pending-returns
  interceptors
  (fn [{:keys [:db]} [offering-address]]
    {:dispatch [:load-auction-pending-returns offering-address
                ;; Active address should be loaded first
                (reverse (sort-by (partial = (:active-address db)) (:my-addresses db)))]}))

(reg-event-fx
  :load-auction-pending-returns
  interceptors
  (fn [{:keys [:db]} [offering-address addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (for [address addresses]
             {:instance (get-instance db :auction-offering offering-address)
              :method :pending-returns
              :args [address]
              :on-success [:pending-returns-loaded offering-address address]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :pending-returns-loaded
  interceptors
  (fn [{:keys [:db]} [offering-address address pending-returns]]
    {:db (assoc-in db
                   [:offering-registry/offerings offering-address :auction-offering/pending-returns address]
                   (d0x-shared-utils/wei->eth->num pending-returns))}))

(reg-event-fx
  :load-ens-records
  interceptors
  (fn [{:keys [:db]} [nodes]]
    {:web3-fx.contract/constant-fns
     {:fns (for [node nodes]
             ;; Can't load all fields at once because it's private at ENS contract, so we load
             ;; just owner, because that's basically all what we need
             {:instance (get-instance db :ens)
              :method :owner
              :args [node]
              :on-success [:ens-record-owner-loaded node]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :ens-record-owner-loaded
  interceptors
  (fn [{:keys [:db]} [node owner]]
    {:db (assoc-in db [:ens/records node :ens.record/owner] owner)}))

(reg-event-fx
  :load-registrar-entry
  interceptors
  (fn [{:keys [:db]} [label-hash]]
    {:web3-fx.contract/constant-fns
     {:fns [{:instance (get-instance db :mock-registrar)
             :method :entries
             :args [label-hash]
             :on-success [:registrar-entry-loaded label-hash]
             :on-error [:district0x.log/error]}]}}))

(reg-event-fx
  :registrar-entry-loaded
  interceptors
  (fn [{:keys [:db]} [label-hash registrar-entry]]
    (let [registrar-entry (parse-registrar-entry registrar-entry {:parse-dates? true :convert-to-ether? true})]
      {:db (update-in db [:registrar/entries label-hash] merge registrar-entry)
       :dispatch [:load-registrar-entry-deed label-hash]})))

(reg-event-fx
  :load-registrar-entry-deed
  interceptors
  (fn [{:keys [:db]} [label-hash]]
    (let [deed-address (get-in db [:registrar/entries label-hash :registrar.entry.deed/address])]
      {:web3-fx.contract/constant-fns
       {:fns [{:instance (get-instance db :deed deed-address)
               :method :value
               :on-success [:registrar-entry-deed-value-loaded label-hash]
               :on-error [:district0x.log/error]}
              {:instance (get-instance db :deed deed-address)
               :method :owner
               :on-success [:registrar-entry-deed-owner-loaded label-hash]
               :on-error [:district0x.log/error]}]}})))

(reg-event-fx
  :registrar-entry-deed-value-loaded
  interceptors
  (fn [{:keys [:db]} [label-hash deed-value]]
    {:db (assoc-in db
                   [:registrar/entries label-hash :registrar.entry.deed/value]
                   (d0x-shared-utils/wei->eth->num deed-value))}))

(reg-event-fx
  :registrar-entry-deed-owner-loaded
  interceptors
  (fn [{:keys [:db]} [label-hash deed-owner]]
    {:db (assoc-in db [:registrar/entries label-hash :registrar.entry.deed/owner] deed-owner)}))

(reg-event-fx
  :search-ens-record-offerings
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens.record/node :ens.record/name]}]]
    (let [node (if name (namehash name) node)]
      {:dispatch [:search-offerings {:search-params {:node node
                                                     :order-by-columns [:created-on]
                                                     :order-by-dirs [:desc]}}]})))

(reg-event-fx
  :search-offering-requests
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

#_(reg-event-fx
    :ens-records.last-offering/loaded
    interceptors
    (fn [{:keys [:db]} [results]]
      (update db :ens/records merge #(hash-map (:node %) {:ens.record/last-offering (:last-offering %)}))))

#_(reg-event-fx
    :search/watched-names
    interceptors
    (fn [{:keys [:db]}]
      (let [nodes (map :ens.record/node (get-form-data db :search-form/watched-names))]
        {:dispatch-n [[:load-nodes-last-offering-ids nodes]
                      [:load-ens-records nodes]]})))

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
                    [:load-ens-records [node]]]})))

(reg-event-fx
  :watched-names/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [node]]
    (let [new-db (update db
                         [:search-form/watched-names :data :watched-names/ens-records]
                         (partial remove #(= node (:ens.record/node %))))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db :search-form/watched-names))})))

#_(reg-event-fx
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


(reg-event-fx
  :set-offerings-search-params-drawer
  interceptors
  (fn [{:keys [:db]} [open?]]
    {:db (assoc-in db [:offerings-search-params-drawer :open?] open?)}))

(reg-event-fx
  :saved-searches/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [saved-searches-key query-string saved-search-name]]
    (let [new-db (assoc-in db [:saved-searches saved-searches-key query-string] saved-search-name)]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:saved-searches]))})))

(reg-event-fx
  :saved-searches/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [saved-searches-key query-string]]
    (let [new-db (medley/dissoc-in db [:saved-searches saved-searches-key query-string])]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:saved-searches]))})))

(reg-event-fx
  :offering-expanded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/address :offering/label-hash :offering/node]}]]
    {:dispatch-n [[:load-registrar-entry label-hash]
                  [:load-my-addresses-auction-pending-returns address]
                  [:load-ens-records [node]]]}))

(reg-event-fx
  :offering-collapsed
  interceptors
  (fn [{:keys [:db]}]
    ))

(reg-event-fx
  :infinite-list/expand-item
  interceptors
  (fn [{:keys [:db]} [key height]]
    {:db (assoc-in db [:infinite-list :expanded-items key :height] height)}))

(reg-event-fx
  :infinite-list/collapse-item
  interceptors
  (fn [{:keys [:db]} [key]]
    {:db (update-in db [:infinite-list :expanded-items] dissoc key)}))


