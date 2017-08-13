(ns district0x.ui.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [cemerick.url :as url]
    [cljs-time.coerce :as time-coerce]
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
    [day8.re-frame.http-fx]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.db]
    [district0x.ui.dispatch-fx]
    [district0x.ui.interval-fx]
    [district0x.ui.location-fx]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db]]
    [district0x.ui.spec]
    [district0x.ui.utils :as d0x-ui-utils]
    [district0x.ui.window-fx]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [medley.core :as medley]
    [print.foo :include-macros true]
    [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console dispatch]]))

(re-frame-storage/reg-co-fx! :contribution {:fx :localstorage :cofx :localstorage})

(defn reg-empty-event-fx [id]
  (reg-event-fx
    id
    (constantly nil)))

(def interceptors [trim-v (validate-db :district0x.ui/db)])

(defn get-contract [db key]
  (get-in db [:smart-contracts key]))

(defn get-instance [db key]
  (get-in db [:smart-contracts key :instance]))

(defn all-contracts-loaded? [db]
  (every? #(and (:abi %) (if goog.DEBUG (:bin %) true)) (vals (:smart-contracts db))))

(defn all-contracts-deployed? [db]
  (every? #(and (:instance %) (:address %)) (vals (:smart-contracts db))))

(defn get-form-data [db form-key & [form-id]]
  (get-in db (remove nil? [form-key form-id :data])))

(defn contract-xhrio [contract-name code-type version on-success on-failure]
  {:method :get
   :uri (gstring/format "./contracts/build/%s.%s?v=%s" contract-name (name code-type) (if goog.DEBUG
                                                                                        (.getTime (js/Date.))
                                                                                        version))
   :timeout 6000
   :response-format (if (= code-type :abi) (ajax/json-response-format) (ajax/text-response-format))
   :on-success on-success
   :on-failure on-failure})

(defn- merge-query-params-into-form [{:keys [:route-handler->form-key] :as db} {:keys [:handler]} query-data]
  (if-let [form-key (route-handler->form-key handler)]
    (update db form-key merge query-data)
    db))

(defn localhost-node? [{:keys [:node-url]}]
  (string/includes? node-url "localhost"))

(defn initialize-db [default-db localstorage]
  (let [web3 (if (and (d0x-ui-utils/provides-web3?)
                      (not (localhost-node? default-db)))
               (new (aget js/window "Web3") (web3/current-provider (aget js/window "web3")))
               (web3/create-web3 (:node-url default-db)))
        load-node-addresses? (if (and (nil? (:load-node-addresses? default-db))
                                      localhost-node?)
                               true
                               (:load-node-addresses? default-db))]
    (as-> default-db db
          (merge-with #(if (map? %1) (merge-with merge %1 %2) %2) db localstorage)
          (assoc db :web3 web3)
          (assoc db :load-node-addresses? load-node-addresses?)
          (merge-query-params-into-form db
                                        (:active-page db)
                                        (d0x-ui-utils/url-query-params->form-data (:form-field->query-param db))))))

(defn- has-tx-status? [tx-status {:keys [:status]}]
  (= tx-status status))

(reg-event-fx
  :district0x/initialize
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [localstorage]} [{:keys [:default-db :conversion-rates :effects]}]]
    (let [db (district0x.ui.events/initialize-db default-db localstorage)
          not-loaded-txs (medley/filter-vals (partial has-tx-status? :tx.status/not-loaded) (:transactions db))
          pending-txs (medley/filter-vals (partial has-tx-status? :tx.status/pending) (:transactions db))]
      (merge
        {:db db
         :ga/page-view [(d0x-ui-utils/current-location-hash)]
         :window/on-resize {:dispatch [:district0x.window/resized]
                            :resize-interval 166}
         :district0x/dispatch-n (remove empty?
                                        [(for [tx-hash (keys not-loaded-txs)]
                                           [:district0x/load-transaction-and-receipt tx-hash])
                                         (for [tx-hash (keys pending-txs)]
                                           [:district0x/load-transaction-receipt tx-hash])])
         ;; On slowed computers injection may not yet happened, so we'll give it some time, just in case
         :dispatch-later [{:ms (if (d0x-ui-utils/provides-web3?) 0 2000) :dispatch [:district0x/load-my-addresses]}]}
        (when conversion-rates
          {:district0x/dispatch [:district0x/load-conversion-rates (:currencies conversion-rates)]
           :dispatch-interval {:dispatch [:district0x/load-conversion-rates (:currencies conversion-rates)]
                               :ms (or (:ms conversion-rates) 60000)
                               :db-path [:district0x/load-conversion-rates-interval]}})

        effects))))

(reg-event-fx
  :district0x/set-active-page
  interceptors
  (fn [{:keys [db]} [{:keys [:handler] :as match}]]
    (merge
      {:db (-> db
             (assoc :active-page match)
             (assoc :drawer-open? false)
             (merge-query-params-into-form match (d0x-ui-utils/url-query-params->form-data (:form-field->query-param db))))
       :ga/page-view [(d0x-ui-utils/current-location-hash)]}
      (when-not (= handler (:handler (:active-page db)))
        {:window/scroll-to-top true}))))

(reg-event-fx
  :district0x/load-my-addresses
  interceptors
  (fn [{:keys [db]}]
    (let [new-db (if (and (d0x-ui-utils/provides-web3?) (not (localhost-node? db)))
                   (assoc db :web3 (aget js/window "web3"))
                   db)]
      (merge
        {:db new-db}
        (if (or (:load-node-addresses? db)
                (d0x-ui-utils/provides-web3?))
          {:web3-fx.blockchain/fns
           {:web3 (:web3 new-db)
            :fns [{:f web3-eth/accounts
                   :on-success [:district0x/my-addresses-loaded]
                   :on-error [:district0x/dispatch-n [[:district0x/set-fallback-web3]
                                                      [:district0x/my-addresses-loaded []]]]}]}}
          {:dispatch [:district0x/my-addresses-loaded []]})))))

(reg-event-fx
  :district0x/load-smart-contracts
  interceptors
  (fn [{:keys [db]} [{:keys [:version]}]]
    {:http-xhrio
     (flatten
       (for [[key {:keys [name]}] (:smart-contracts db)]
         (for [code-type (if goog.DEBUG [:abi :bin] [:abi])]
           (contract-xhrio name
                           code-type
                           version
                           [:district0x/smart-contract-loaded key code-type]
                           [:district0x.log/error :district0x/load-smart-contracts]))))}))

(reg-event-fx
  :district0x/clear-smart-contracts
  interceptors
  (fn [{:keys [db]}]
    {:db (update db :smart-contracts (partial medley/map-kv
                                              (fn [contract-key contract]
                                                [contract-key (dissoc contract :abi :bin :address :instance)])))}))

(reg-event-fx
  :district0x/smart-contract-loaded
  interceptors
  (fn [{:keys [db]} [contract-key code-type code]]
    (let [code (if (= code-type :abi) (clj->js code) (str "0x" code))
          contract (get-contract db contract-key)
          contract-address (:address contract)]
      (let [new-db (cond-> db
                     true
                     (assoc-in [:smart-contracts contract-key code-type] code)

                     (= code-type :abi)
                     (update-in [:smart-contracts contract-key] merge
                                (when contract-address
                                  {:instance (web3-eth/contract-at (:web3 db) code contract-address)})))]
        (merge
          {:db new-db
           :district0x/dispatch-n [(when (all-contracts-loaded? new-db)
                                     [:district0x/smart-contracts-loaded])]})))))

(reg-empty-event-fx :district0x/smart-contracts-loaded)

(reg-event-fx
  :district0x/my-addresses-loaded
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [addresses]]
    (let [active-address (if (contains? (set addresses) (:active-address localstorage))
                           (:active-address localstorage)
                           (first addresses))]
      {:db (-> db
             (assoc :my-addresses addresses)
             (assoc :active-address active-address))})))

(reg-event-fx
  :district0x/deploy-contract
  interceptors
  (fn [{:keys [db]} [{:keys [:address-index :contract-key :on-success :args :gas] :as params
                      :or {gas 4500000}}]]
    (let [contract (get-contract db contract-key)
          tx-opts {:gas gas
                   :data (:bin contract)
                   :from (if address-index
                           (nth (:my-addresses db) address-index)
                           (:active-address db))}]
      {:web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns [{:f web3-eth/contract-new
               :args (concat [(:abi contract)] args [tx-opts])
               :on-success [:district0x/contract-deployed (select-keys params [:contract-key :on-success])]
               :on-error [:district0x.log/error :district0x/deploy-contract contract-key]}]}})))

(reg-event-fx
  :district0x/contract-deployed
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [{:keys [:on-success :contract-key]} instance]]
    (when-let [contract-address (aget instance "address")]
      (console :log contract-key " deployed at " contract-address)
      (merge
        {:db (update-in db [:smart-contracts contract-key] merge {:address contract-address :instance instance})
         :localstorage (assoc-in localstorage [:smart-contracts contract-key] {:address contract-address})}
        (when on-success
          {:dispatch on-success})))))

(reg-event-fx
  :district0x/watch-eth-balances
  interceptors
  (fn [{:keys [db]} [{:keys [:addresses :on-address-balance-loaded]
                      :or {on-address-balance-loaded [:district0x/address-balance-loaded :eth]}}]]
    (when (seq addresses)
      {:web3-fx.blockchain/balances
       {:web3 (:web3 db)
        :watch? true
        :blockchain-filter-opts "latest"
        :db-path [:district0x/watch-eth-balances]
        :addresses addresses
        :dispatches [on-address-balance-loaded
                     [:district0x/blockchain-connection-error :district0x/watch-eth-balances]]}})))

(reg-event-fx
  :district0x/load-eth-balances
  interceptors
  (fn [{:keys [db]} [{:keys [:addresses :on-address-balance-loaded]
                      :or {on-address-balance-loaded [:district0x/address-balance-loaded :eth]}}]]
    (when (seq addresses)
      {:web3-fx.blockchain/balances
       {:web3 (:web3 db)
        :addresses addresses
        :dispatches [on-address-balance-loaded
                     [:district0x/blockchain-connection-error :district0x/watch-token-balances]]}})))

(reg-event-fx
  :district0x/watch-my-eth-balances
  interceptors
  (fn [{:keys [db]} [args]]
    (let [addresses (:my-addresses db)]
      {:dispatch [:district0x/watch-eth-balances (assoc args :addresses addresses)]})))

(reg-event-fx
  :district0x/watch-token-balances
  interceptors
  (fn [{:keys [db]} [{:keys [:addresses :instance :token-code]}]]
    (when (seq addresses)
      {:web3-fx.blockchain/balances
       {:web3 (:web3 db)
        :watch? true
        :blockchain-filter-opts "latest"
        :db-path [:district0x/watch-token-balances]
        :addresses addresses
        :instance instance
        :dispatches [[:district0x/address-balance-loaded token-code]
                     [:district0x/blockchain-connection-error :district0x/watch-token-balances]]}})))

(reg-event-fx
  :district0x/load-token-balances
  interceptors
  (fn [{:keys [db]} [{:keys [:addresses :instance :token-code]}]]
    (when (seq addresses)
      {:web3-fx.blockchain/balances
       {:web3 (:web3 db)
        :addresses addresses
        :instance instance
        :dispatches [[:district0x/address-balance-loaded token-code]
                     [:district0x/blockchain-connection-error :district0x/watch-token-balances]]}})))

(reg-event-fx
  :district0x/watch-my-token-balances
  interceptors
  (fn [{:keys [db]} [args]]
    (let [addresses (:my-addresses db)]
      {:dispatch [:district0x/watch-token-balances (assoc args :addresses addresses)]})))

(reg-event-fx
  :district0x/address-balance-loaded
  interceptors
  (fn [{:keys [db]} [token balance address :as a]]
    {:db (assoc-in db [:balances address token] (d0x-shared-utils/big-num->ether balance))}))

(reg-event-fx
  :district0x/load-conversion-rates
  interceptors
  (fn [{:keys [db]} [currencies]]
    {:http-xhrio {:method :get
                  :uri (str "https://min-api.cryptocompare.com/data/price?fsym=ETH&tsyms="
                            (string/join "," (map name currencies)))
                  :timeout 20000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:district0x/conversion-rates-loaded]
                  :on-failure [:district0x.log/error :district0x/conversion-rates-loaded]}}))

(reg-event-db
  :district0x/conversion-rates-loaded
  interceptors
  (fn [db [response]]
    (update db :conversion-rates merge response)))

(reg-event-fx
  :district0x/set-active-address
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [address]]
    {:db (-> db
           (assoc :active-address address))
     :localstorage (assoc localstorage :active-address address)}))

(reg-event-db
  :district0x.form/set-value
  [interceptors (conform-args (s/cat :form-key :form/key
                                     :form-id (s/? :form/id)
                                     :field-key :form/field
                                     :value any?
                                     :validator (s/? (s/or :validator-fn fn?
                                                           :valid? boolean?))))]
  (fn [db [{:keys [:form-key :form-id :field-key :value :validator]}]]
    (let [validator (d0x-shared-utils/resolve-conformed-spec-or {:valid? constantly} validator)]
      (cond-> db
        true (assoc-in (remove nil? [form-key form-id :data field-key]) value)

        (or (and validator (validator value))
            (nil? validator))
        (update-in (remove nil? [form-key form-id :errors]) (comp set (partial remove #{field-key})))

        (and validator (not (validator value)))
        (update-in (remove nil? [form-key form-id :errors]) conj field-key)))))

(reg-event-fx
  :district0x.form/submit
  interceptors
  (fn [{:keys [db]} [{:keys [:form-key :form-data :form-id :tx-opts :contract-key :contract-method
                             :wei-keys :form-data-order] :as props}]]
    (let [{:keys [:web3 :active-address :contract-method-configs :form-configs]} db
          form-config (form-configs form-key)
          contract-method (or contract-method (:contract-method form-config))
          contract-method-config (contract-method-configs (contract-method form-config))
          form-data-order (or form-data-order (:form-data-order contract-method-config))
          wei-args (or wei-keys (:wei-keys contract-method-config))
          contract-key (or contract-key (keyword (namespace contract-method)))
          tx-opts (merge
                    {:from active-address}
                    (:tx-opts form-config)
                    tx-opts)]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [{:instance (if-let [contract-address (:contract-address form-data)]
                           (web3-eth/contract-at web3 (:abi (get-contract db contract-key)) contract-address)
                           (get-instance db contract-key))
               :method contract-method
               :args (-> (d0x-shared-utils/map-selected-keys-vals d0x-shared-utils/eth->wei wei-args form-data)
                       (d0x-shared-utils/map->vec form-data-order))
               :tx-opts tx-opts
               :on-success [:district0x/dispatch-n
                            [:district0x/load-transaction]
                            [:district0x.transaction-log/add props]]
               :on-error [:district0x.log/error :district0x.form/submit form-key form-data tx-opts]
               :on-tx-receipt [:district0x.form/tx-receipt-loaded (:gas tx-opts) props]}]}})))

(reg-event-fx
  :district0x.form/tx-receipt-loaded
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-receipt :on-tx-receipt-n :form-data :on-error]
                       :or {on-error [:district0x.snackbar/show-transaction-error]}}
                      {:keys [:transaction-hash :gas-used] :as tx-receipt}]]
    (let [success? (not= (get-in db [:transactions transaction-hash :gas]) gas-used)]
      (merge
        {:district0x/dispatch [:district0x/transaction-receipt-loaded tx-receipt]}
        (when (and success? on-tx-receipt)
          {:dispatch (concat on-tx-receipt [form-data tx-receipt])})
        (when (and success? on-tx-receipt-n)
          {:dispatch-n (map #(concat % [form-data tx-receipt]) on-tx-receipt-n)})
        (when-not (and success? on-error)
          {:dispatch (concat on-tx-receipt [form-data tx-receipt])})))))

(reg-event-fx
  :district0x/load-transaction
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [{:f web3-eth/get-transaction
             :args [transaction-hash]
             :on-success [:district0x/transaction-loaded]
             :on-error [:district0x.log/error :district0x/load-transaction transaction-hash]}]}}))

(reg-event-fx
  :district0x/transaction-loaded
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:gas :hash] :as transaction}]]
    (let [gas-used (get-in db [:transactions hash :gas-used])
          transaction (assoc transaction :status (if gas-used
                                                   (if (= gas-used gas)
                                                     :tx.status/failure
                                                     :tx.status/success)
                                                   :tx.status/pending))]
      {:dispatch [:district0x.transaction-log/update hash transaction]})))

(reg-event-fx
  :district0x/load-transaction-receipt
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [{:f web3-eth/get-transaction-receipt
             :args [transaction-hash]
             :on-success [:district0x/transaction-receipt-loaded]
             :on-error [:district0x.log/error :district0x/load-transaction-receipt transaction-hash]}]}}))

(reg-event-fx
  :district0x/transaction-receipt-loaded
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:gas-used :transaction-hash] :as tx-receipt}]]
    (let [gas-limit (get-in db [:transactions transaction-hash :gas])
          tx-receipt (assoc tx-receipt :status (if gas-limit
                                                 (if (= gas-limit gas-used)
                                                   :tx.status/failure
                                                   :tx.status/success)
                                                 :tx.status/not-loaded))]
      {:dispatch [:district0x.transaction-log/update transaction-hash tx-receipt]})))

(reg-event-fx
  :district0x/load-transaction-and-receipt
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash]]
    {:dispatch-n [[:district0x/load-transaction transaction-hash]
                  [:district0x/load-transaction-receipt transaction-hash]]}))


(reg-event-fx
  :district0x-emails/set-email
  interceptors
  (fn [{:keys [:db]} [form-data submit-props]]
    {:dispatch [:district0x.form/submit
                (merge
                  {:form-key :form.district0x-emails/set-email
                   :contract-key :district0x-emails
                   :form-data form-data
                   :form-data-order [:district0x-emails/email]}
                  submit-props)]}))

(reg-event-fx
  :district0x.transaction-log/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [{:keys [:form-key :tx-opts :form-id] :as props} tx-hash]]
    (let [new-db (-> db
                   (assoc-in [:transactions tx-hash] (merge (select-keys props [:form-key :form-data :form-id :tx-opts])
                                                            {:created-on (t/now)
                                                             :hash tx-hash
                                                             :state :tx.status/not-loaded}))
                   (update :transaction-ids-chronological conj tx-hash)
                   (update-in (remove nil? [:transaction-ids-by-form form-key (:from tx-opts) form-id]) tx-hash))]
      {:db new-db
       :localstorage (merge localstorage
                            (select-keys new-db [:transactions
                                                 :transaction-ids-chronological
                                                 :transaction-ids-by-form]))})))

(reg-event-fx
  :district0x.transaction-log/update
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} transaction-hash transaction]
    (let [new-db (update-in db
                            [:transactions transaction-hash]
                            merge
                            (-> transaction
                              (select-keys [:block-hash :gas-used :gas :value :status])
                              (update :value bn/->number)))]
      (let [{:keys [:gas :gas-used :form-data]} (get-in new-db [:transactions transaction-hash])]
        (when (and gas gas-used)
          (println transaction-hash gas gas-used (str (* (/ gas gas-used) 100) "%")))
        (when form-data
          (print.foo/look form-data)))
      (merge
        {:db new-db
         :localstorage (merge localstorage (select-keys new-db [:transactions]))}))))

(reg-event-fx
  :district0x.transaction-log/clear
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]}]
    (let [cleared-txs (select-keys district0x.ui.db/default-db [:transactions
                                                                :transaction-ids-chronological
                                                                :transaction-ids-by-form])]
      {:db (merge db cleared-txs)
       :localstorage (merge localstorage cleared-txs)})))

(s/def :district0x.form/error-fx (s/cat :form-key :form/key
                                        :form-id (s/? :form/id)
                                        :error keyword?))

(reg-event-fx
  :district0x.form/add-error
  [interceptors (conform-args :district0x.form/error-fx)]
  (fn [db {:keys [:form-key :form-id :error]}]
    (update-in db (remove nil? [form-key form-id :errors]) conj error)))

(reg-event-db
  :district0x.form/remove-error
  [interceptors (conform-args :district0x.form/error-fx)]
  (fn [db {:keys [:form-key :form-id :error]}]
    (update-in db (remove nil? [form-key form-id :errors]) (comp set (partial remove #{error})))))

(reg-event-fx
  :district0x.contract/event-watch-once
  interceptors
  (fn [{:keys [:db]} [{:keys [:contract-key :on-success :on-error] :as event-params
                       :or {on-error [:district0x.log/error]}}]]
    (let [event-id (str "event-listen-once-" (rand-int 99999))
          db-path [:web3-event-filters]]
      {:web3-fx.contract/events
       {:db-path db-path
        :events [(merge event-params
                        {:instance (get-instance db contract-key)
                         :event-id event-id
                         :on-success [:district0x.contract/event-stop-watching-once db-path event-id on-success]
                         :on-error [:district0x.contract/event-stop-watching-once db-path event-id on-error]})]}})))

(reg-event-fx
  :district0x.contract/event-stop-watching-once
  interceptors
  (fn [{:keys [:db]} [db-path event-id dispatch & args]]
    {:web3-fx.contract/events-stop-watching {:db-path db-path
                                             :event-ids [event-id]}
     :dispatch (vec (concat (d0x-shared-utils/collify dispatch) args))}))


(reg-event-fx
  :district0x.contract/state-fn-call
  interceptors
  (fn [{:keys [db]} [{:keys [:contract-key :method :args :tx-opts] :as opts}]]
    (let [tx-opts (merge {:gas 3800000
                          :from (:active-address db)}
                         tx-opts)]
      {:web3-fx.contract/state-fns
       {:web3 (:web3 db)
        :db-path [:contract/state-fns]
        :fns [{:instance (get-instance db contract-key)
               :method method
               :args args
               :tx-opts tx-opts
               :on-success [:district0x.log/info]
               :on-error [:district0x.log/error method]
               :on-tx-receipt [:district0x.form/tx-receipt-loaded (:gas tx-opts) (assoc opts :fn-key method)]}]}})))

(reg-event-fx
  :district0x.server/http-get
  interceptors
  (fn [{:keys [db]} [{:keys [:http-xhrio :params :on-success :endpoint]} :as opts]]
    {:http-xhrio (merge
                   {:method :get
                    :timeout 20000
                    :uri (str (url/url (:server-url db) endpoint))
                    :response-format (ajax/json-response-format)
                    :on-success on-success
                    :on-failure [:district0x.log/error]
                    :params (if (:order-by params)
                              (update params :order-by #(partition-all 2 (interleave %)))
                              params)}
                   http-xhrio)}))

(reg-event-fx
  :district0x.search-results/load
  interceptors
  (fn [{:keys [db]} [{:keys [:http-xhrio :search-params :search-results-key :clear-existing-ids? :endpoint :form-key
                             :on-success] :as opts}]]
    {:db (update-in db [search-results-key search-params] merge (merge {:loading? true}
                                                                       (when clear-existing-ids?
                                                                         {:ids []})))
     :dispatch [:district0x.server/http-get {:http-xhrio http-xhrio
                                             :endpoint endpoint
                                             :params (if form-key
                                                       (get-form-data db form-key)
                                                       search-params)
                                             :on-success [:district0x.search-results/loaded opts]}]}))

(reg-event-fx
  :district0x.search-results/loaded
  interceptors
  (fn [{:keys [db]} [{:keys [:search-results-key :search-params :id-key :on-success]} results]]
    (let [ids (if id-key (map #(get % id-key)) results)]
      (merge
        {:db (update-in db [search-results-key (dissoc search-params :select-fields)] merge {:loading? false :ids ids})}
        (when on-success
          {:dispatch (vec (concat on-success [ids results]))})))))

(reg-event-fx
  :district0x.search-results/load-multi
  interceptors
  (fn [{:keys [db]} [{:keys [:http-xhrio :search-params-fn :search-params-key :search-results-key :clear-existing-ids?
                             :endpoint :form-key :on-success :request-params] :as opts}]]
    {:db (update db search-results-key (partial merge-with merge) (zipmap
                                                                    (vals search-params-fn)
                                                                    (repeat (count search-params-fn)
                                                                            (merge {:loading? true}
                                                                                   (when clear-existing-ids?
                                                                                     {:ids []})))))
     :dispatch [:district0x.server/http-get {:http-xhrio http-xhrio
                                             :endpoint endpoint
                                             :params (if form-key
                                                       (get-form-data db form-key)
                                                       request-params)
                                             :on-success [:district0x.search-results/multi-loaded opts]}]}))

(reg-event-fx
  :district0x.search-results/multi-loaded
  interceptors
  (fn [{:keys [db]} [{:keys [:search-results-key search-params-fn :search-params-key :id-key :on-success]} results]]
    (merge
      {:db (update db search-results-key merge (map #(hash-map (search-params-fn %)
                                                               (d0x-shared-utils/collify (get % id-key)))
                                                    results))}
      (when on-success
        {:dispatch (vec (concat on-success [(map #(get % id-key) results) results]))}))))

(reg-event-fx
  :district0x.contract/constant-fn-call
  interceptors
  (fn [{:keys [db]} [contract-key & args]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat [(get-instance db contract-key)] args [[:district0x.log/info]
                                                           [:district0x.log/error]])]}}))

(reg-event-fx
  :district0x/clear-localstorage
  interceptors
  (fn [_]
    {:localstorage nil}))

(reg-event-fx
  :district0x.log/print-db
  interceptors
  (fn [{:keys [:db]}]
    (print.foo/look db)
    nil))

(reg-event-fx
  :district0x.log/error
  interceptors
  (fn [{:keys [:db]} errors]
    (apply console :error errors)
    {:db db
     :ga/event ["error" (first errors) (str (rest errors))]}))

(reg-event-fx
  :district0x.log/info
  interceptors
  (fn [db result]
    (apply console :log (bn/->numbers (if (and (not (string? result)) (some sequential? result))
                                        (map bn/->numbers result)
                                        result)))))

(reg-event-db
  :district0x.snackbar/close
  interceptors
  (fn [db _]
    (assoc-in db [:snackbar :open?] false)))

(reg-event-fx
  :district0x.snackbar/show-message
  interceptors
  (fn [{:keys [db]} [message]]
    {:db (update db :snackbar merge
                 {:open? true
                  :message message
                  :action nil
                  :on-action-touch-tap nil})}))

(reg-event-fx
  :district0x.snackbar/show-transaction-error
  interceptors
  (fn [{:keys [db]}]
    {:dispatch [:district0x.snackbar/show-message
                {:message "Sorry, your transaction hasn't been processed"}]}))

(reg-event-fx
  :district0x.snackbar/show-message-redirect-action
  interceptors
  (fn [{:keys [db]} [message route route-params routes]]
    {:db (update db :snackbar merge
                 {:open? true
                  :message message
                  :action "SHOW ME"
                  :on-action-touch-tap #(dispatch [:district0x.location/nav-to route route-params])})}))

(reg-event-db
  :district0x.dialog/close
  interceptors
  (fn [db _]
    (assoc-in db [:dialog :open?] false)))

(reg-event-fx
  :district0x/blockchain-connection-error
  interceptors
  (fn [{:keys [:db]} errors]
    (apply console :error "Blockchain Connection Error:" errors)
    {:db (assoc db :blockchain-connection-error? true)
     :dispatch [:district0x.snackbar/show-message "Sorry, we have trouble connecting into the Ethereum blockchain"]}))

(reg-event-fx
  :district0x/unlock-account
  interceptors
  (fn [{:keys [db]} [address password seconds]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [[web3-personal/unlock-account address password (or seconds 999999)
             [:district0x.log/info]
             [:district0x.log/error :blockchain/unlock-account]]]}}))

(reg-event-fx
  :district0x.location/set-query
  interceptors
  (fn [_ args]
    {:location/set-query args}))

(reg-event-fx
  :district0x.location/nav-to
  interceptors
  (fn [{:keys [:db]} [route route-params]]
    {:location/nav-to [route route-params (:routes db)]}))

(reg-fx
  :district0x.location/add-to-query
  (fn [_ args]
    (:location/add-to-query args)))

(reg-event-fx
  :district0x.window/resized
  interceptors
  (fn [{:keys [db]} [width]]
    {:db (assoc db :window-width-size (d0x-ui-utils/get-window-width-size width))}))

(reg-event-fx
  :district0x.window/scroll-to-top
  interceptors
  (fn []
    {:window/scroll-to-top true}))

(reg-event-fx
  :district0x/set-ui-disabled
  interceptors
  (fn [{:keys [:db]} [disabled?]]
    {:db (assoc db :ui-disabled? disabled?)}))

(reg-event-fx
  :district0x/async-flow
  interceptors
  (fn [_ [params]]
    {:async-flow params}))

(reg-event-fx
  :district0x/dispatch-n
  interceptors
  (fn [_ [params]]
    {:dispatch-n params}))

