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
    [name-bazaar.utils :refer [namehash sha3]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v]]
    [clojure.string :as str]))

(def check-spec-interceptor (after (partial district0x.events/check-and-throw :name-bazaar.db/db)))
(def interceptors [trim-v check-spec-interceptor])

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


