(ns name-bazaar.ui.events.ens-events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address?]]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 parse-query-params path-for get-node-name get-offering-name get-offering auction-offering?]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [district0x.shared.utils :as d0x-shared-utils]))

(reg-event-fx
  :ens/set-owner
  [interceptors (validate-first-arg (s/keys :req [:ens.record/owner :ens.record/name]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (assoc form-data :ens.record/node (namehash (:ens.record/name form-data)))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Set ENS owner for %s" (:ens.record/name form-data))
                   :contract-key :ens
                   :contract-method :set-owner
                   :form-data form-data
                   :args-order [:ens.record/node :ens.record/owner]
                   :result-href (path-for :route.ens-record/detail form-data)
                   :form-id (select-keys form-data [:ens.record/node])
                   :tx-opts {:gas 100000 :gas-price default-gas-price}}]})))

(reg-event-fx
  :ens.records/load
  interceptors
  (fn [{:keys [:db]} [nodes {:keys [:load-resolver?]}]]
    (let [instance (get-instance db :ens)]
      {:web3-fx.contract/constant-fns
       {:fns (concat
               (for [node nodes]
                 {:instance instance
                  :method :owner
                  :args [node]
                  :on-success [:ens.records.owner/loaded node]
                  :on-error [:district0x.log/error]})
               (when load-resolver?
                 (for [node nodes]
                   {:instance instance
                    :method :resolver
                    :args [node]
                    :on-success [:ens.records.resolver/loaded node]
                    :on-error [:district0x.log/error]})))}})))

(reg-event-fx
  :ens.records.owner/loaded
  interceptors
  (fn [{:keys [:db]} [node owner]]
    {:db (assoc-in db [:ens/records node :ens.record/owner] (if (= owner "0x")
                                                              d0x-shared-utils/zero-address
                                                              owner))}))

(reg-event-fx
  :ens.records.resolver/loaded
  interceptors
  (fn [{:keys [:db]} [node resolver]]
    {:db (assoc-in db [:ens/records node :ens.record/resolver] (if (= resolver "0x")
                                                                 d0x-shared-utils/zero-address
                                                                 resolver))}))