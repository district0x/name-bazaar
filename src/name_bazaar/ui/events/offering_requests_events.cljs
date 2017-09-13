(ns name-bazaar.ui.events.offering-requests-events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [parse-offering-request top-level-name?]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash normalize path-for update-search-results-params]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(reg-event-fx
  :offering-requests/add-request
  [interceptors (validate-first-arg (s/keys :req [:ens.record/name]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (update form-data :ens.record/name normalize)
          node (namehash (:ens.record/name form-data))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Request for %s" (:ens.record/name form-data))
                   :contract-key :offering-requests
                   :contract-method :add-request
                   :form-data form-data
                   :args-order [:ens.record/name]
                   :result-href (path-for :route.ens-record/detail form-data)
                   :form-id (select-keys form-data [:ens.record/name])
                   :tx-opts {:gas 120000 :gas-price default-gas-price}
                   :on-tx-receipt-n [[:offering-requests/load [node]]
                                     [:offering-requests.has-requested/load node (:my-addresses db)]]}]})))

(reg-event-fx
  :offering-requests/search
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:search-results-path [:search-results :offering-requests :main-search]
                   :endpoint "/offering-requests"
                   :on-success [:offering-requests/load]}
                  (assoc-in opts [:params :total-count?] true))]}))

(reg-event-fx
  :offering-requests/load
  interceptors
  (fn [{:keys [:db]} [nodes]]
    (let [instance (get-instance db :offering-requests)]
      {:web3-fx.contract/constant-fns
       {:fns (for [node nodes]
               {:instance instance
                :method :get-request
                :args [node]
                :on-success [:offering-requests/loaded node]
                :on-error [:district0x.log/error]})}})))

(reg-event-fx
  :offering-requests/loaded
  interceptors
  (fn [{:keys [:db]} [node offering-request]]
    (let [offering-request (parse-offering-request offering-request)]
      {:db (update-in db [:offering-requests node] merge (merge offering-request
                                                                {:offering-request/node node}))})))

(reg-event-fx
  :offering-requests.has-requested/load
  interceptors
  (fn [{:keys [:db]} [node addresses]]
    {:web3-fx.contract/constant-fns
     {:fns [{:instance (get-instance db :offering-requests)
             :method :has-requested
             :args [node addresses]
             :on-success [:offering-requests.has-requested/loaded node addresses]
             :on-error [:district0x.log/error]}]}}))

(reg-event-fx
  :offering-requests.has-requested/loaded
  interceptors
  (fn [{:keys [:db]} [node addresses has-requested-vals]]
    (let [{addrs-requested true addrs-not-requested false} (->> (zipmap addresses has-requested-vals)
                                                             (group-by second)
                                                             (medley/map-vals keys))]
      {:db (update-in db [:offering-requests node :offering-request/requesters]
                      (fn [requesters]
                        (-> (set requesters)
                          (set/difference addrs-not-requested)
                          (set/union addrs-requested)
                          set)))})))

(reg-event-fx
  :offering-requests.main-search/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-results-path [:search-results :offering-requests :main-search]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)]
      {:db db
       :dispatch [:offering-requests/search {:search-results-path search-results-path
                                             :append? (:append? opts)
                                             :params search-params}]})))

(reg-event-fx
  :offering-requests.list-item/expanded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering-request/name]}]]
    {:dispatch [:name/load-all-details name]}))
