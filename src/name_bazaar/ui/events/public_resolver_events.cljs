(ns name-bazaar.ui.events.public-resolver-events
  (:require
    [cljs-web3.core :as web3]
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address? merge-in]]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils :refer [truncate path-with-query]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [reverse-record-node namehash sha3 parse-query-params path-for get-ens-record-name get-offering-name get-offering]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))



(reg-event-fx
 :public-resolver.addr/load
 interceptors
 (fn [{:keys [:db]} [node]]
   (let [instance (get-instance db :public-resolver)]
     {:web3-fx.contract/constant-fns
      {:fns
       [{:instance instance
         :method :addr
         :args [node]
         :on-success [:public-resolver.addr/loaded node]
         :on-error [:district0x.log/error]}]}})))

(reg-event-fx
 :public-resolver.addr/loaded
 interceptors
 (fn [{:keys [:db]} [node addr]]
   (when-not (empty-address? addr)
     {:db (assoc-in db [:public-resolver/records node :public-resolver.record/addr] addr)})))

(reg-event-fx
 :public-resolver.name/load
 interceptors
 (fn [{:keys [:db]} [addr]]
   (when (and (not (empty-address? addr))
              (web3/address? addr))
     (let [node (reverse-record-node addr)
           instance (get-instance db :public-resolver)]
       {:web3-fx.contract/constant-fns
        {:fns
         [{:instance instance
           :method :name
           :args [node]
           :on-success [:public-resolver.name/loaded addr]
           :on-error [:district0x.log/error]}]}}))))

(reg-event-fx
 :public-resolver.name/loaded
 interceptors
 (fn [{:keys [:db]} [addr name]]
   (when (and name
              (not= name ""))
     {:db (assoc-in db [:public-resolver/reverse-records
                        addr
                        :public-resolver.record/name] name)})))

(reg-event-fx
 :public-resolver/set-addr
 [interceptors (validate-first-arg (s/keys :req [:ens.record/name
                                                 :ens.record/addr]))]
 (fn [{:keys [:db]} [form-data]]
   (let [instance (get-instance db :ens)
         public-resolver (get-in db [:smart-contracts :public-resolver :address])
         form-data (assoc form-data
                          :ens.record/node (namehash (str (:ens.record/name form-data)
                                                          constants/registrar-root))
                          :public-resolver (get form-data :ens.record/resolver public-resolver))]
     {:dispatch [:district0x/make-transaction
                 {:name (gstring/format "Point %s to %s"
                                        (:ens.record/name form-data)
                                        (:ens.record/addr form-data))
                  :contract-key :public-resolver
                  :contract-method :set-addr
                  :form-data (select-keys form-data [:ens.record/node :ens.record/addr])
                  :args-order [:ens.record/node :public-resolver]
                  :result-href (path-for :route.ens-record/detail form-data)
                  :form-id (select-keys form-data [:ens.record/node])
                  :tx-opts {:gas 100000 :gas-price default-gas-price}
                  :on-tx-receipt-n [[:ens.records.resolver/load [(:ens.record/node form-data)]]
                                    [:district0x.snackbar/show-message
                                      (gstring/format "%s is now pointing to %s"
                                                      (:ens.record/name form-data)
                                                      (truncate (:ens.record/addr form-data)
                                                                10))]]}]})))

(reg-event-fx
 :public-resolver/set-name
  [interceptors (validate-first-arg (s/keys :req [:ens.record/addr]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (-> form-data
                        (update :ens.record/name #(str % constants/registrar-root))
                        (assoc :ens.record/node (reverse-record-node (:ens.record/addr form-data))))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Point %s to %s"
                                         (truncate (:ens.record/addr form-data) 10)
                                         (:ens.record/name form-data))
                   :contract-key :public-resolver
                   :contract-method :set-name
                   :form-data (select-keys form-data [:ens.record/node :ens.record/name])
                   :args-order [:ens.record/node :ens.record/name]
                   :form-id (select-keys form-data [:ens.record/node])
                   :tx-opts {:gas 100000 :gas-price default-gas-price}
                   :on-tx-receipt-n [[:public-resolver.name/load (:ens.record/addr form-data)]
                                     [:ens.records.resolver/load [(:ens.record/node form-data)]]
                                     [:district0x.snackbar/show-message
                                      (gstring/format "%s is pointed to %s."
                                                      (truncate (:ens.record/addr form-data)
                                                                10)
                                                      (:ens.record/name form-data))]]}]})))
