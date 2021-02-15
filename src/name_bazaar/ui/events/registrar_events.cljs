(ns name-bazaar.ui.events.registrar-events
  (:require
    [bignumber.core :as bn]
    [cljs.spec.alpha :as s]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [district0x.shared.utils :refer [wei->eth->num eth->wei empty-address? rand-str zero-address zero-address evm-time->date-time]]
    [district0x.ui.events :as d0x-ui-events]
    [district0x.ui.spec-interceptors :refer [validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [sha3 seal-bid normalize path-for]]
    [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path after dispatch trim-v console subscribe]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(reg-event-fx
  :name-bazaar-registrar/transfer
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label :ens.record/owner]))]
  (fn [{:keys [:db]} [form-data opts]]
    (let [active-address (subscribe [:district0x/active-address])
          form-data (assoc form-data :ens.record/label-hash (sha3 (:ens.record/label form-data))
                                     :name-bazaar-registrar.registration/owner @active-address)
          name (str (:ens.record/label form-data) constants/registrar-root)]
      {:dispatch [:district0x/make-transaction
                  (merge
                    {:name (gstring/format "Transfer %s ownership" name)
                     :contract-key :name-bazaar-registrar
                     :contract-method :transfer-from
                     :form-data form-data
                     :args-order [:name-bazaar-registrar.registration/owner :ens.record/owner :ens.record/label-hash]
                     :form-id (select-keys form-data [:ens.record/label])
                     :tx-opts {:gas 75000 :gas-price default-gas-price}}
                    opts)]})))

(reg-event-fx
  :name-bazaar-registrar/register
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label]))]
  (fn [{:keys [:db]} [form-data]]
    (let [label (normalize (:ens.record/label form-data))
          ens-record-name (str label constants/registrar-root)
          form-data (assoc form-data :ens.record/label-hash (sha3 label))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Register %s" ens-record-name)
                   :contract-key :name-bazaar-registrar
                   :contract-method :register
                   :form-data form-data
                   :result-href (path-for :route.ens-record/detail {:ens.record/name ens-record-name})
                   :args-order [:ens.record/label-hash]
                   :tx-opts {:gas 700000 :gas-price default-gas-price}
                   :on-tx-receipt [:district0x.snackbar/show-message
                                   (gstring/format "%s was successfully registered" ens-record-name)]}]})))

(reg-event-fx
  :name-bazaar-registrar.registrations/load
  interceptors
  (fn [{:keys [:db]} [label-hashes]]
    (let [instance (d0x-ui-events/get-instance db :name-bazaar-registrar)]
      {:web3-fx.contract/constant-fns
       {:fns (flatten (for [label-hash label-hashes]
                                [{:instance instance
                                  :method :available
                                  :args [label-hash]
                                  :on-success [:name-bazaar-registrar.registration/available-loaded label-hash]
                                  :on-error [:district0x.log/error]}
                                 {:instance instance
                                  :method :name-expires
                                  :args [label-hash]
                                  :on-success [:name-bazaar-registrar.registration/expiry-loaded label-hash]
                                  :on-error [:district0x.log/error]}
                                 {:instance instance
                                  :method :owner-of
                                  :args [label-hash]
                                  :on-success [:name-bazaar-registrar.registration/owner-loaded label-hash]
                                  ;; ownerOf contract call can fail if the name is not owned
                                  ;; in that case, we want to set zero address to the db.
                                  :on-error [:name-bazaar-registrar.registration/owner-loaded
                                             label-hash
                                             zero-address]}
                                 ]))}})))

(reg-event-fx
  :name-bazaar-registrar.registration/available-loaded
  interceptors
  (fn [_ [label-hash value]]
    {:dispatch [:name-bazaar-registrar.registration/loaded
                label-hash
                :name-bazaar-registrar.registration/available
                value]}))

(reg-event-fx
  :name-bazaar-registrar.registration/expiry-loaded
  interceptors
  (fn [_ [label-hash value]]
    {:dispatch [:name-bazaar-registrar.registration/loaded
                label-hash
                :name-bazaar-registrar.registration/expiration-date
                (evm-time->date-time (bn/number value))]}))

(reg-event-fx
  :name-bazaar-registrar.registration/owner-loaded
  interceptors
  (fn [_ [label-hash value]]
    {:dispatch [:name-bazaar-registrar.registration/loaded
                label-hash
                :name-bazaar-registrar.registration/owner
                value]}))

(reg-event-fx
  :name-bazaar-registrar.registration/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash db-keyword value]]
    {:db (assoc-in db [:name-bazaar-registrar/registrations label-hash db-keyword] value)}))
