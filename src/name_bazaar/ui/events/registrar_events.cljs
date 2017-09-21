(ns name-bazaar.ui.events.registrar-events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address?]]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 normalize parse-query-params path-for get-ens-record-name get-offering-name get-offering]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [district0x.shared.utils :as d0x-shared-utils]))

(reg-event-fx
  :registrar/transfer
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label :ens.record/owner]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (assoc form-data :ens.record/label-hash (sha3 (:ens.record/label form-data)))
          name (str (:ens.record/label form-data) constants/registrar-root)]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Transfer %s ownership" name)
                   :contract-key :mock-registrar #_:registrar ;; TODO handling mock-registrar vs registrar
                   :contract-method :transfer
                   :form-data form-data
                   :result-href (path-for :route.offerings/detail {:offering/address (:ens.record/owner form-data)})
                   :args-order [:ens.record/label-hash :ens.record/owner]
                   :form-id (select-keys form-data [:ens.record/label])
                   :tx-opts {:gas 100000 :gas-price default-gas-price}
                   :on-tx-receipt-n [[:offerings.ownership/load [(:ens.record/owner form-data)]]
                                     [:district0x.snackbar/show-message
                                      (gstring/format "Ownership of %s was transferred" name)]]}]})))

(reg-event-fx
  :registrar/register
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label]))]
  (fn [{:keys [:db]} [form-data]]
    (let [label (normalize (:ens.record/label form-data))
          ens-record-name (str label constants/registrar-root)
          form-data (assoc form-data :ens.record/label-hash (sha3 label))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Register %s" ens-record-name)
                   :contract-key :mock-registrar
                   :contract-method :register
                   :form-data form-data
                   :result-href (path-for :route.ens-record/detail {:ens.record/name ens-record-name})
                   :args-order [:ens.record/label-hash]
                   :tx-opts {:gas 700000 :gas-price default-gas-price}
                   :on-tx-receipt [:district0x.snackbar/show-message
                                   (gstring/format "%s was successfully registered" ens-record-name)]}]})))

(reg-event-fx
  :registrar.entries/load
  interceptors
  (fn [{:keys [:db]} [label-hashes]]
    (let [instance (get-instance db :mock-registrar)]
      {:web3-fx.contract/constant-fns
       {:fns (for [label-hash label-hashes]
               {:instance instance
                :method :entries
                :args [label-hash]
                :on-success [:registrar.entry/loaded label-hash]
                :on-error [:district0x.log/error]})}})))

(reg-event-fx
  :registrar.entry/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash registrar-entry]]
    (let [registrar-entry (parse-registrar-entry registrar-entry {:parse-dates? true :convert-to-ether? true})]
      {:db (update-in db [:registrar/entries label-hash] merge registrar-entry)
       :dispatch [:registrar.entry.deed/load label-hash]})))

(reg-event-fx
  :registrar.entry.deed/load
  interceptors
  (fn [{:keys [:db]} [label-hash]]
    (let [deed-address (get-in db [:registrar/entries label-hash :registrar.entry.deed/address])]
      (when-not (empty-address? deed-address)
        {:web3-fx.contract/constant-fns
         {:fns [{:instance (get-instance db :deed deed-address)
                 :method :value
                 :on-success [:registrar-entry.deed.value/loaded label-hash]
                 :on-error [:district0x.log/error]}
                {:instance (get-instance db :deed deed-address)
                 :method :owner
                 :on-success [:registrar-entry.deed.owner/loaded label-hash]
                 :on-error [:district0x.log/error]}]}}))))

(reg-event-fx
  :registrar-entry.deed.value/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash deed-value]]
    {:db (assoc-in db
                   [:registrar/entries label-hash :registrar.entry.deed/value]
                   (d0x-shared-utils/wei->eth->num deed-value))}))

(reg-event-fx
  :registrar-entry.deed.owner/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash deed-owner]]
    {:db (assoc-in db [:registrar/entries label-hash :registrar.entry.deed/owner]
                   (when-not (empty-address? deed-owner) deed-owner))}))





