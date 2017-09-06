(ns name-bazaar.ui.events.offering-requests-events
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
  :offering-requests/add-request
  [interceptors (validate-first-arg (s/keys :req [:ens.record/name]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Request for %s" (:ens.record/name form-data))
                 :contract-key :offering-requests
                 :contract-method :add-request
                 :form-data form-data
                 :args-order [:ens.record/name]
                 :result-href (path-for :route.ens-record/detail form-data)
                 :form-id (select-keys form-data [:ens.record/name])
                 :tx-opts {:gas 100000 :gas-price default-gas-price}}]}))

(reg-event-fx
  :offering-requests/search
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:search-results-path [:search-results :offering-requests :main-search]
                   :endpoint "/offering-requests"
                   :on-success [:offering-requests/load]}
                  opts)]}))

(reg-event-fx
  :offering-requests/load
  interceptors
  (fn [{:keys [:db]} [nodes]]
    (let [[nodes-with-known-names nodes-with-unknown-names] (split-with (partial get-node-name db) nodes)]
      {:web3-fx.contract/constant-fns
       {:fns
        (remove nil?
                [(when (seq nodes-with-known-names)
                   {:instance (get-contract db :offering-requests)
                    :method :get-requests-counts
                    :args [nodes-with-known-names]
                    :on-success [:offering-requests/counts-loaded nodes-with-known-names]
                    :on-error [:district0x.log/error]})
                 (when (seq nodes-with-unknown-names)
                   {:instance (get-contract db :offering-requests)
                    :method :get-requests
                    :args [nodes-with-unknown-names]
                    :on-success [:offering-requests/loaded nodes-with-unknown-names]
                    :on-error [:district0x.log/error]})])}})))

(reg-event-fx
  :offering-requests/counts-loaded
  interceptors
  (fn [{:keys [:db]} [nodes counts]]
    (let [counts (map (comp (partial hash-map :offering-request/requesters-count) bn/->number) counts)]
      {:db (update-in db :offering-requests
                      (partial merge-with merge)
                      (parse-offering-requests-counts nodes counts))})))

(reg-event-fx
  :offering-requests/loaded
  interceptors
  (fn [{:keys [:db]} [nodes [names counts]]]
    {:db (update db :ens/records
                 (partial merge-with merge)
                 (zipmap nodes (map (partial hash-map :ens.record/name) names)))
     :dispatch [:offering-requests/counts-loaded nodes counts]}))

(reg-event-fx
  :offering-requests.has-requested/load
  interceptors
  (fn [{:keys [:db]} [node addresses]]
    (let [addresses (if-not addresses (:my-addresses db) addresses)]
      {:web3-fx.contract/constant-fns
       {:fns [{:instance (get-contract db :offering-requests)
               :method :has-requested
               :args [node addresses]
               :on-success [:offering-requests.has-requested/loaded node addresses]
               :on-error [:district0x.log/error]}]}})))

(reg-event-fx
  :offering-requests.has-requested/loaded
  interceptors
  (fn [{:keys [:db]} [node addresses has-requested-vals]]
    (let [[addrs-requested addrs-not-requested] (->> (zipmap addresses has-requested-vals)
                                                  (split-with second)
                                                  (map (partial map first)))]
      {:db (update-in db [:offering-requests node :offering-request/requesters]
                      (fn [requesters]
                        (-> (set requesters)
                          (set/difference addrs-not-requested)
                          (set/union addrs-requested))))})))

(reg-event-fx
  :offering/set-settings-tx-receipt
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/name :offering/address]}]]
    {:dispatch [:district0x.snackbar/show-message-redirect-action
                {:message (str "Offering for " name " was updated!")
                 :route :route.offerings/detail
                 :route-params {:offering/address address}
                 :routes constants/routes}]}))


(reg-event-fx
  :offering/reclaim-ownership
  [interceptors (validate-first-arg (s/keys :req [:offering/address]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Reclaim ownership from %s offering"
                                       (get-offering-name db (:offering/address form-data)))
                 :contract-key :buy-now-offering
                 :contract-method :reclaim-ownership
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :result-href (path-for :route.offerings/detail form-data)
                 :form-id (select-keys form-data [:offering/address])
                 :tx-opts {:gas 200000 :gas-price default-gas-price}}]}))
