(ns name-bazaar.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [cljs-time.coerce :as time-coerce]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs-web3.utils :as web3-utils]
    [cljs.spec :as s]
    [clojure.data :as data]
    [clojure.set :as set]
    [clojure.string :as string]
    [day8.re-frame.async-flow-fx]
    [district0x.big-number :as bn]
    [district0x.debounce-fx]
    [district0x.events :refer [get-contract get-instance all-contracts-loaded?]]
    [district0x.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.constants :as constants]
    [name-bazaar.utils :refer [namehash]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch]]))

(def interceptors district0x.events/interceptors)

(reg-event-fx
  :deploy-ens
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :ens
                                             :on-success [:ens-deployed]}]}))

(reg-event-fx
  :ens-deployed
  (constantly nil))

(reg-event-fx
  :deploy-fifs-registrar
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :fifs-registrar
                                             :args [(:address (get-contract db :ens)) (namehash "")]
                                             :on-success [:fifs-registrar-deployed]}]}))

(reg-event-fx
  :fifs-registrar-deployed
  (constantly nil))

#_ (reg-event-fx
  :deploy-ens-node-names
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :ens-node-names
                                             :on-success [:ens-node-names-deployed]}]}))

#_ (reg-event-fx
  :ens-node-names-deployed
  (constantly nil))

(reg-event-fx
  :deploy-offering-registry
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :offering-registry
                                             :on-success [:offering-registry-deployed]}]}))
(reg-event-fx
  :offering-registry-deployed
  (constantly nil))


(reg-event-fx
  :deploy-offering-requests
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :offering-requests
                                             :on-success [:offering-requests-deployed]}]}))
(reg-event-fx
  :offering-requests-deployed
  (constantly nil))

(reg-event-fx
  :deploy-instant-buy-offering-factory
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :instant-buy-offering-factory
                                             :args (map (comp :address (partial get-contract db))
                                                        [:ens :offering-registry :offering-requests])
                                             :on-success [:instant-buy-offering-factory-deployed]}]}))
(reg-event-fx
  :instant-buy-offering-factory-deployed
  (constantly nil))


(reg-event-fx
  :deploy-english-auction-offering-factory
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :english-auction-offering-factory
                                             :args (map (comp :address (partial get-contract db))
                                                        [:ens :offering-registry :offering-requests])
                                             :on-success [:english-auction-offering-factory-deployed]}]}))
(reg-event-fx
  :english-auction-offering-factory-deployed
  (constantly nil))

(reg-event-fx
  :used-by-factories/set-factories
  interceptors
  (fn [{:keys [db]} [{:keys [:contract-key]}]]
    {:dispatch [:district0x.contract/state-fn-call {:contract-key contract-key
                                                    :contract-method :set-factories
                                                    :args [(map (comp :address (partial get-contract db))
                                                                [:instant-buy-offering-factory
                                                                 :english-auction-offering-factory])
                                                           true]
                                                    :transaction-opts {:gas 100000}}]}))

(reg-event-fx
  :instant-buy-offering-factory/create-offering
  interceptors
  (fn [{:keys [:db]} [form-data {:keys [:address]}]]
    {:dispatch [:district0x.form/submit
                {:form-data form-data
                 :address address
                 :fn-key :instant-buy-offering-factory/create-offering
                 :fn-args (constants/contracts-method-args :instant-buy-offering-factory/create-offering)
                 :wei-args constants/contracts-method-wei-args
                 :form-key :form.instant-buy-offering-factory/create-offering}]}))

(reg-event-fx
  :english-auction-offering-factory/create-offering
  interceptors
  (fn [{:keys [:db]} [form-data {:keys [:address]}]]
    {:dispatch [:district0x.form/submit
                {:form-data form-data
                 :address address
                 :fn-key :instant-buy-offering-factory/create-offering
                 :fn-args (constants/contracts-method-args :english-auction-offering-factory/create-offering)
                 :wei-args constants/contracts-method-wei-args
                 :form-key :form.english-auction-offering-factory/create-offering}]}))

(reg-event-fx
  :reinitialize
  interceptors
  (fn [{:keys [:db]} args]
    (let [{:keys [:my-addresses]} db]
      (.clear js/console)
      {:dispatch [:district0x/clear-smart-contracts]
       :async-flow {:first-dispatch [:district0x/load-smart-contracts]
                    :rules [{:when :seen?
                             :events [:district0x/smart-contracts-loaded]
                             :dispatch-n [[:deploy-ens]
                                          [:deploy-offering-registry]
                                          [:deploy-offering-requests]]}
                            #_{:when :seen?
                               :events [:ens-deployed]
                               :dispatch [:deploy-fifs-registrar]}
                            #_ {:when :seen?
                             :events [:ens-node-names-deployed]
                             :dispatch [:deploy-offering-requests]}
                            {:when :seen?
                             :events [:ens-deployed :offering-registry-deployed :offering-requests-deployed]
                             :dispatch-n [[:deploy-instant-buy-offering-factory]
                                          [:deploy-english-auction-offering-factory]]}
                            {:when :seen?
                             :events [:instant-buy-offering-factory-deployed :english-auction-offering-factory-deployed]
                             :dispatch-n [[:used-by-factories/set-factories {:contract-key :offering-registry}]
                                          [:used-by-factories/set-factories {:contract-key :offering-requests}]]
                             :halt? true}]}})))

(reg-event-fx
  :ens/set-owner
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens/name :ens/owner :ens/node]}]]
    {:dispatch [:district0x.contract/state-fn-call {:contract-key :ens
                                                    :contract-method :set-owner
                                                    :args [(if name (namehash name) node) owner]
                                                    :transaction-opts {:gas 100000}}]}))



(reg-event-fx
  :generate-db
  interceptors
  (fn [{:keys [:db]} args]
    (let [{:keys [:my-addresses]} db]
      (.clear js/console)
      {:async-flow {:first-dispatch [:district0x/load-smart-contracts]
                    :rules []}})))