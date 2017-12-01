(ns name-bazaar.ui.events.ens-events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address? merge-in]]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [reverse-record-node namehash sha3 parse-query-params path-for get-ens-record get-offering-name get-offering]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [district0x.shared.utils :as d0x-shared-utils]
    [medley.core :as medley]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(reg-event-fx
  :ens/set-owner
  [interceptors (validate-first-arg (s/keys :req [:ens.record/owner :ens.record/name]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (assoc form-data :ens.record/node (namehash (:ens.record/name form-data)))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Transfer %s ownership" (:ens.record/name form-data))
                   :contract-key :ens
                   :contract-method :set-owner
                   :form-data form-data
                   :args-order [:ens.record/node :ens.record/owner]
                   :result-href (path-for :route.ens-record/detail form-data)
                   :form-id (select-keys form-data [:ens.record/node])
                   :tx-opts {:gas 100000 :gas-price default-gas-price}
                   :on-tx-receipt [:district0x.snackbar/show-message
                                   (gstring/format "Ownership of %s was transferred" (:ens.record/name form-data))]}]})))

(reg-event-fx
  :ens.records/load
  interceptors
  (fn [{:keys [:db]} [nodes {:keys [:load-resolver?]}]]
    (let [instance (get-instance db :ens)]
      (info [:LOADING-NODES nodes instance])
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
  :ens.records.active-offerings/load
  interceptors
  (fn [{:keys [:db]} [nodes]]
    (when (seq nodes)
      {:dispatch [:district0x.server/http-get {:endpoint "/offerings"
                                               :params {:nodes nodes
                                                        :select-fields [:node :address]
                                                        :node-owner? true}
                                               :on-success [:ens.records.active-offerings/loaded nodes]
                                               :on-failure [:district0x.log/error]}]})))

(reg-event-fx
  :ens.records.owner/loaded
  interceptors
  (fn [{:keys [:db]} [node owner]]
    (info ["Loaded owner" node owner])
    {:db (assoc-in db [:ens/records node :ens.record/owner] (if (= owner "0x")
                                                              d0x-shared-utils/zero-address
                                                              owner))}))

(reg-event-fx
 :ens.records.resolver/load
 interceptors
 (fn [{:keys [:db]} [nodes]]
   (let [instance (get-instance db :ens)]
     (info [:LOADING-RESOLVER nodes instance])
     {:web3-fx.contract/constant-fns
      {:fns (for [node nodes]
              {:instance instance
               :method :resolver
               :args [node]
               :on-success [:ens.records.resolver/loaded node]
               :on-error [:district0x.log/error]})}})))

(reg-event-fx
  :ens.records.resolver/loaded
  interceptors
  (fn [{:keys [:db]} [node resolver]]
    (info ["Loaded resolver" node resolver])
    (when (and node resolver)
      {:db (assoc-in db [:ens/records node :ens.record/resolver] (if (= resolver "0x")
                                                                   d0x-shared-utils/zero-address
                                                                   resolver))})))

(reg-event-fx
  :ens.records.active-offerings/loaded
  interceptors
  (fn [{:keys [:db]} [all-nodes {active-offerings :items}]]
    ;; active-offerings contains only nodes that have active offerings, all other nodes need to
    ;; reset active-offering, so we don't display outdated info
    (let [emptied-active-offerings (reduce #(assoc %1 %2 {:ens.record/active-offering nil}) {} all-nodes)
          active-offerings (reduce #(assoc %1 (:node %2) {:ens.record/active-offering (:address %2)})
                                   emptied-active-offerings
                                   active-offerings)]
      {:db (update db :ens/records merge-in active-offerings)})))

(reg-event-fx
  :ens.records.owner/resolve
  interceptors
  (fn [{:keys [:db]} [node]]
    (let [{:keys [:ens.record/owner]} (get-ens-record db node)]
      {:dispatch [:public-resolver.name/load owner]})))

(reg-event-fx
  :ens.records/setup-public-resolver
  [interceptors (validate-first-arg (s/keys :req [:ens.record/name]))]
  (fn [{:keys [:db]} [form-data]]
    (let [public-resolver (get-in db [:smart-contracts :public-resolver :address])
          form-data (assoc form-data
                           :ens.record/node (namehash (str (:ens.record/name form-data)
                                                           constants/registrar-root))
                           :public-resolver public-resolver)]
      (info [:SET-RESOLVER-NODES name public-resolver])
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Setting resolver for %s" (:ens.record/name form-data))
                   :contract-key :ens
                   :contract-method :set-resolver
                   :form-data (select-keys form-data [:ens.record/node :public-resolver])
                   :args-order [:ens.record/node :public-resolver]
                   :form-id (select-keys form-data [:ens.record/node])
                   :tx-opts {:gas 100000 :gas-price default-gas-price}
                   :on-tx-receipt [:district0x.snackbar/show-message
                                   (gstring/format "Resolver for %s is set to standard." (:ens.record/name form-data))]}]})))

(reg-event-fx
  :ens.records/setup-public-reverse-resolver
  [interceptors (validate-first-arg (s/keys :req [:ens.record/address]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Setting reverse resolver for %s" (:ens.record/address form-data))
                 :contract-key :reverse-registrar
                 :contract-method :claim-with-resolver
                 :form-data (select-keys form-data [:ens.record/address :public-resolver])
                 :args-order [:ens.record/address :public-resolver]
                 :form-id (select-keys form-data [:ens.record/address])
                 :tx-opts {:gas 100000 :gas-price default-gas-price}
                 :on-tx-receipt [:district0x.snackbar/show-message
                                 (gstring/format "Resolver for %s is set to standard." (:ens.record/address form-data))]}]}))
