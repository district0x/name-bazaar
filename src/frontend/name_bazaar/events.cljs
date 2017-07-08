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
    [cljs.spec :as s]
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
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch]]
    [clojure.string :as str]))

(def interceptors district0x.events/interceptors)

(reg-event-fx
  :deploy-ens
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :ens
                                             :on-success [:ens-deployed]}]}))

(reg-empty-event-fx :ens-deployed)

(reg-event-fx
  :deploy-fifs-registrar
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :fifs-registrar
                                             :args [(:address (get-contract db :ens)) (namehash "")]
                                             :on-success [:fifs-registrar-deployed]}]}))

(reg-empty-event-fx :fifs-registrar-deployed)

#_(reg-event-fx
    :deploy-ens-node-names
    interceptors
    (fn [{:keys [db]} [{:keys [:address-index]}]]
      {:dispatch [:district0x/deploy-contract {:address-index address-index
                                               :contract-key :ens-node-names
                                               :on-success [:ens-node-names-deployed]}]}))

#_(reg-event-fx
    :ens-node-names-deployed
    (constantly nil))

(reg-event-fx
  :deploy-offering-registry
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :offering-registry
                                             :on-success [:offering-registry-deployed]}]}))

(reg-empty-event-fx :offering-registry-deployed)

(reg-event-fx
  :deploy-offering-requests
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :offering-requests
                                             :on-success [:offering-requests-deployed]}]}))

(reg-empty-event-fx :offering-requests-deployed)

(reg-event-fx
  :deploy-instant-buy-offering-factory
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index :offering-factory/emergency-multisig]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :instant-buy-offering-factory
                                             :args (-> (mapv (comp :address (partial get-contract db))
                                                             [:ens :offering-registry :offering-requests])
                                                     (conj emergency-multisig))
                                             :on-success [:instant-buy-offering-factory-deployed]}]}))

(reg-empty-event-fx :instant-buy-offering-factory-deployed)

(reg-event-fx
  :deploy-offering-library
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :offering-library
                                             :on-success [:offering-library-deployed]}]}))

(defn- link-contract-library [db contract-key library-contract-key]
  (let [library-address (:address (get-contract db library-contract-key))
        placeholder (constants/library-placeholders library-contract-key)]
    (update-in db [:smart-contracts contract-key :bin]
               (fn [bin]
                 (str/replace bin placeholder (subs library-address 2))))))

(reg-event-fx
  :offering-library-deployed
  interceptors
  (fn [{:keys [db]}]
    (let [library-address (:address (get-contract db :offering-library))]
      {:db (-> db
             (link-contract-library :instant-buy-offering-factory :offering-library)
             (link-contract-library :english-auction-offering-factory :offering-library)
             (link-contract-library :english-auction-offering-library :offering-library)
             (link-contract-library :instant-buy-offering-library :offering-library))})))


(reg-event-fx
  :deploy-instant-buy-offering-library
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :instant-buy-offering-library
                                             :on-success [:instant-buy-offering-library-deployed]}]}))

(reg-event-fx
  :instant-buy-offering-library-deployed
  interceptors
  (fn [{:keys [db]}]
    (let [library-address (:address (get-contract db :instant-buy-offering-library))]
      {:db (link-contract-library db :instant-buy-offering-factory :instant-buy-offering-library)})))

(reg-event-fx
  :deploy-english-auction-offering-library
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :english-auction-offering-library
                                             :on-success [:english-auction-offering-library-deployed]}]}))

(reg-event-fx
  :english-auction-offering-library-deployed
  interceptors
  (fn [{:keys [db]}]
    (let [library-address (:address (get-contract db :english-auction-offering-library))]
      {:db (link-contract-library db :english-auction-offering-factory :english-auction-offering-library)})))

(reg-event-fx
  :deploy-english-auction-offering-factory
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index :offering-factory/emergency-multisig]}]]
    {:dispatch [:district0x/deploy-contract {:address-index address-index
                                             :contract-key :english-auction-offering-factory
                                             :args (-> (mapv (comp :address (partial get-contract db))
                                                             [:ens :offering-registry :offering-requests])
                                                     (conj emergency-multisig))
                                             :on-success [:english-auction-offering-factory-deployed]}]}))

(reg-empty-event-fx :english-auction-offering-factory-deployed)

(reg-event-fx
  :used-by-factories/set-factories
  interceptors
  (fn [{:keys [db]} [params]]
    {:dispatch [:district0x.contract/state-fn-call (merge {:method :set-factories
                                                           :args [(map (comp :address (partial get-contract db))
                                                                       [:instant-buy-offering-factory
                                                                        :english-auction-offering-factory])
                                                                  true]
                                                           :transaction-opts {:gas 100000}}
                                                          params)]}))

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
                 :fn-key :english-auction-offering-factory/create-offering
                 :fn-args (constants/contracts-method-args :english-auction-offering-factory/create-offering)
                 :wei-args constants/contracts-method-wei-args
                 :form-key :form.english-auction-offering-factory/create-offering}]}))

(reg-event-fx
  :ens/set-owner
  interceptors
  (fn [{:keys [:db]} [form-data {:keys [:address]}]]
    (let [form-data (cond-> form-data
                      (:ens/name form-data) (assoc :ens/node (namehash (:ens/name form-data))))]
      {:dispatch [:district0x.form/submit
                  {:form-data form-data
                   :address address
                   :fn-key :ens/set-owner
                   :fn-args (constants/contracts-method-args :ens/set-owner)
                   :form-key :form.ens/set-owner
                   :form-id (:ens/node form-data)}]})))

(reg-event-fx
  :ens/set-subnode-owner
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens/root-node :ens/label :ens/owner]
                       :or {root-node ""}}]]
    {:dispatch [:district0x.contract/state-fn-call
                {:contract-key :ens
                 :method :set-subnode-owner
                 :args [(namehash root-node) (sha3 label) owner]
                 :on-tx-receipt [:ens/set-subnode-owner-success]}]}))

(reg-event-fx
  :ens/set-subnode-owner-success
  (constantly nil))

(reg-event-fx
  :watch-offering-added
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens/node :ens/owner :on-success :on-error]}]]
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
           (update-in [:ens/nodes node] (fn [ens-node]
                                          (merge ens-node
                                                 {:node/owner owner
                                                  :node/offerings (conj (vec (:node/offerings ens-node))
                                                                        offering)}))))}))

(reg-event-fx
  :transfer-node-owner-to-latest-offering
  interceptors
  (fn [{:keys [:db]} [{:keys [:ens/name]}]]
    (let [node (namehash name)
          offering (last (get-in db [:ens/nodes node :node/offerings]))]
      {:dispatch [:ens/set-owner {:ens/node node :ens/owner offering}]})))

(reg-event-fx
  :reinitialize
  interceptors
  (fn [{:keys [:db]} args]
    (let [{:keys [:my-addresses :active-address]} db]
      (reg-empty-event-fx :offering-registry-factories-set)
      (reg-empty-event-fx :offering-requests-factories-set)
      (reg-empty-event-fx :reinitialize-success)
      (.clear js/console)
      {:dispatch [:district0x/clear-smart-contracts]
       :async-flow {:first-dispatch [:district0x/load-smart-contracts]
                    :rules [{:when :seen?
                             :events [:district0x/smart-contracts-loaded]
                             :dispatch-n [[:deploy-ens]
                                          [:deploy-offering-registry]
                                          [:deploy-offering-requests]
                                          [:deploy-offering-library]]}
                            #_{:when :seen?
                               :events [:ens-deployed]
                               :dispatch [:deploy-fifs-registrar]}
                            #_{:when :seen?
                               :events [:ens-node-names-deployed]
                               :dispatch [:deploy-offering-requests]}
                            {:when :seen?
                             :events [:offering-library-deployed]
                             :dispatch-n [[:deploy-instant-buy-offering-library]
                                          [:deploy-english-auction-offering-library]]}
                            {:when :seen?
                             :events [:ens-deployed
                                      :offering-registry-deployed
                                      :offering-requests-deployed
                                      :instant-buy-offering-library-deployed
                                      :english-auction-offering-library-deployed]
                             :dispatch-n [[:deploy-instant-buy-offering-factory
                                           {:offering-factory/emergency-multisig active-address}]
                                          [:deploy-english-auction-offering-factory
                                           {:offering-factory/emergency-multisig active-address}]]}
                            {:when :seen?
                             :events [:instant-buy-offering-factory-deployed :english-auction-offering-factory-deployed]
                             :dispatch-n [[:used-by-factories/set-factories
                                           {:contract-key :offering-registry
                                            :on-tx-receipt [:offering-registry-factories-set]}]
                                          [:used-by-factories/set-factories
                                           {:contract-key :offering-requests
                                            :on-tx-receipt [:offering-requests-factories-set]}]]}
                            {:when :seen?
                             :events [:offering-registry-factories-set :offering-requests-factories-set]
                             :dispatch [:reinitialize-success]
                             :halt? true}]}})))

(reg-event-fx
  :generate-db
  interceptors
  (fn [{:keys [:db]} args]
    (let [{:keys [:my-addresses]} db]
      {:dispatch [:district0x/set-ui-disabled true]
       :async-flow {:first-dispatch [:reinitialize]
                    :rules [{:when :seen?
                             :events [:reinitialize-success]
                             :dispatch [:ens/set-subnode-owner {:ens/label "eth"
                                                                :ens/owner (:active-address db)}]}
                            {:when :seen?
                             :events [:ens/set-subnode-owner-success]
                             :dispatch-n [[:watch-offering-added]
                                          [:instant-buy-offering-factory/create-offering
                                           {:instant-buy-offering-factory/name "eth"
                                            :instant-buy-offering/price 0.1}]
                                          [:english-auction-offering-factory/create-offering
                                           {:english-auction-offering-factory/name "eth"
                                            :english-auction-offering-factory/start-price 0.01
                                            :english-auction-offering-factory/start-time 1 #_(to-epoch (t/now))
                                            :english-auction-offering-factory/end-time (to-epoch (t/plus (t/now) (t/years 1)))
                                            :english-auction-offering-factory/extension-duration (t/in-seconds (t/hours 1))
                                            :english-auction-offering-factory/extension-trigger-duration (t/in-seconds (t/hours 1))
                                            :english-auction-offering-factory/min-bid-increase 0.01}]]
                             :halt? true}
                            #_{:when :seen?
                               :events [:offering-registry/on-offering-added]
                               :dispatch-n [[:transfer-node-owner-to-latest-offering {:ens/name "eth"}]
                                            [:district0x/set-ui-disabled false]]
                               :halt? true}]}})))


