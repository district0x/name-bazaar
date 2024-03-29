(ns district0x.ui.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [bignumber.core :as bn]
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
    [district.encryption :as encryption]
    [district.shared.error-handling :refer [try-catch]]
    [district.ui.logging.events :as logging]
    [district0x.shared.utils :as d0x-shared-utils :refer [wei->eth]]
    [district0x.ui.db]
    [district0x.ui.dispatch-fx]
    [district0x.ui.history :as history]
    [district0x.ui.history-fx]
    [district0x.ui.interval-fx]
    [district0x.ui.location-fx]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [district0x.ui.spec]
    [district0x.ui.utils :as d0x-ui-utils :refer [get-window-size to-locale-string current-location-hash namehash file-write]]
    [district0x.ui.web3-fx :as web3-fx.ethereum]
    [district0x.ui.window-fx]
    [goog.object]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [medley.core :as medley]
    [print.foo :include-macros true]
    [print.foo :refer [look]]
    [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console dispatch reg-cofx]]
    [re-promise.core]
    [taoensso.timbre :as log]
    ))

(re-frame-storage/reg-co-fx! :contribution {:fx :localstorage :cofx :localstorage})

(defn reg-empty-event-fx [id]
  (reg-event-fx
    id
    (constantly nil)))

(reg-cofx
  :current-url
  (fn [coeffects _]
    (assoc coeffects :current-url (d0x-ui-utils/current-url))))

(def interceptors [trim-v (validate-db :district0x.ui/db)])

(defn get-contract [db key]
  (get-in db [:smart-contracts key]))

(defn get-instance
  ([db key]
   (get-in db [:smart-contracts key :instance]))
  ([db key address]
   (let [abi (:abi (get-contract db key))]
     (web3-eth/contract-at (:web3 db) abi address))))

(defn all-contracts-loaded? [db]
  (every? #(and (:abi %) (if goog.DEBUG (:bin %) true)) (vals (:smart-contracts db))))

(defn contract-xhrio [contract-name version on-success on-failure]
  {:method :get
   :uri (gstring/format "./contracts-build/%s.json?v=%s" contract-name (if goog.DEBUG
                                                                         (.getTime (js/Date.))
                                                                         version))
   :timeout 60000
   :response-format (ajax/json-response-format)
   :on-success on-success
   :on-failure on-failure})

(defn create-wallet-connect-web3
  "Initiate web3 modal popup and allow user to choose a way how to
  connect to Namebazaar. If the modal is closed or there is an
  unexpected error it tries to create web3 from fallback url.

  The result of the web3 modal will be web3 provider which is used
  to create new web3 instance (which is later saved into DB). This
  handling is asynchronous and the function returns a promise."
  [infura-id fallback-web3-url]
  ;; Clear the wallet connect local storage, which persists previous connection
  ;; I've found the persisted connection to be extremely unreliable
  (.removeItem (.-localStorage js/window) "walletconnect")
  ;; https://lwhorton.github.io/2018/10/20/clojurescript-interop-with-javascript.html
  (let [web3-modal-class (goog.object/get (goog.object/get js/window "Web3Modal") "default")
        wallet-connect-provider (goog.object/get (goog.object/get js/window "WalletConnectProvider") "default")
        ;; Inspired by https://github.com/Web3Modal/web3modal-vanilla-js-example/blob/master/example.js#L52
        provider-options (clj->js {:walletconnect {:package wallet-connect-provider :options {:infuraId infura-id }}})
        web3-modal-options (clj->js {:cacheProvider false :providerOptions provider-options :disableInjectedProvider false})
        web3-modal (new web3-modal-class web3-modal-options)]
    (->
      (js-invoke web3-modal "connect")
      (.then (fn [provider] (new (aget js/window "Web3") provider)))
      (.catch (fn [err]
                (log/error "Unable to retrieve web3 provider via wallet connect. Creating fallback web3. Reason:" err)
                (web3/create-web3 fallback-web3-url))))))

(defn initialize-db
  "Update re-frame `db` with `localstorage` and `current-url` co-effects.

  Notes:
  - Returned `db` is used as a `:db` side-effect."
  [db localstorage current-url]
  (-> db
      (d0x-shared-utils/merge-in localstorage)
      (update :active-page merge
              {:query-params (medley/map-keys keyword (:query current-url))
               :path (:path current-url)})
      (update-in [:transaction-log :settings] merge
                 {:open? false
                  :highlighted-transaction nil})))

(defn- contains-tx-status? [tx-statuses {:keys [:status]}]
  (contains? tx-statuses status))

(reg-event-fx
  :district0x/db-and-web3-initialized
  [trim-v]
  (fn [{:keys [db]} [conversion-rates effects web3]]
    (let [db (assoc db :web3 web3)
          transactions (get-in db [:transaction-log :transactions])
          txs-to-reload (medley/filter-vals #(contains-tx-status? #{:tx.status/not-loaded :tx.status/pending} %)
                                            transactions)]
      (merge
        {:db db
         :ga/page-view [(if history/hashroutes?
                          (d0x-ui-utils/current-location-hash)
                          (history/get-state))]
         :window/on-resize {:dispatch [:district0x.window/resized]
                            :resize-interval 166}
         :window/on-focus {:dispatch [:district0x.window/set-focus true]}
         :window/on-blur {:dispatch [:district0x.window/set-focus false]}
         :district0x/dispatch-n (vec (concat
                                       (for [tx-hash (keys txs-to-reload)]
                                         [:district0x/load-transaction-receipt tx-hash])
                                       (for [tx-hash (keys txs-to-reload)]
                                         [:web3-fx.contract/add-transaction-hash-to-watch
                                          {:web3 (:web3 db)
                                           :db-path [:contract/state-fns]
                                           :transaction-hash tx-hash
                                           :on-tx-receipt [:district0x/on-tx-receipt {}]}])))
         :dispatch-later [{:ms 0 :dispatch [::logging/info "Initialized web3 instance"]}
                          {:ms 0 :dispatch [:district0x/load-my-addresses]}
                          {:ms 0 :dispatch [:district0x/setup-address-reload-interval]}]}
        (when conversion-rates
          {:district0x/dispatch [:district0x/load-conversion-rates (:currencies conversion-rates)]
           :dispatch-interval {:dispatch [:district0x/load-conversion-rates (:currencies conversion-rates)]
                               :ms (or (:ms conversion-rates) 60000)
                               :db-path [:district0x/load-conversion-rates-interval]}})

        effects))
    ))

(reg-event-fx
  :district0x/initialize
  [interceptors (inject-cofx :localstorage) (inject-cofx :current-url)]
  (fn [{:keys [:localstorage :current-url]} [{:keys [:default-db :conversion-rates :effects]}]]
    (let [db (initialize-db default-db localstorage current-url)]
      {:db db
       :promise {:call #(create-wallet-connect-web3 (:infura-id db) (:fallback-web3-url db))
                 :on-success [:district0x/db-and-web3-initialized conversion-rates effects]}})))

(reg-event-fx
  :district0x/set-current-location-as-active-page
  interceptors
  (fn [_ args]
    {:dispatch [:district0x/set-active-page (apply d0x-ui-utils/match-current-location args)]}))

(reg-event-fx
  :district0x/set-active-page
  [interceptors (inject-cofx :current-url)]
  (fn [{:keys [:db :current-url]} [{:keys [:handler] :as match}]]
    (let [{:keys [:query :path]} current-url]
      (merge
        {:db (-> db
                 (assoc :active-page (merge match {:query-params (medley/map-keys keyword (:query current-url))
                                                   :path path}))
                 (assoc-in [:menu-drawer :open?] false))
         :ga/page-view [(if history/hashroutes?
                          (d0x-ui-utils/current-location-hash)
                          (history/get-state))]}
        (when-not (= handler (:handler (:active-page db)))
          {:window/scroll-to-top true})))))

(reg-event-fx
  :district0x/load-my-addresses
  interceptors
  (fn [{:keys [db]}]
      (merge
        (if (:load-node-addresses? db)
          {:web3-fx.blockchain/fns
           {:web3 (:web3 db)
            :fns [{:f web3-eth/accounts
                   :on-success [:district0x/my-addresses-loaded]
                   :on-error [:district0x/dispatch-n [[::logging/info "No addresses could be loaded" :district0x/load-my-addresses]
                                                      [:district0x/my-addresses-loaded []]]]}]}}
          {:dispatch [:district0x/my-addresses-loaded []]}))))

(reg-event-fx
  :district.server.config/load
  interceptors
  (fn [{:keys [db]} _]
    (let [uri (str (url/url (:server-url db) "/config"))]
      {:db db
       :http-xhrio {:method :get
                    :uri uri
                    :timeout 30000                          ;; 30 seconds
                    :response-format (ajax/transit-response-format)
                    :on-success [:district.server.config/loaded]
                    :on-failure [::logging/error "Failed to load config" {:uri uri} :district.server.config/load]}})))

(reg-event-db
  :district.server.config/loaded
  interceptors
  (fn [db [config]]
    (assoc-in db [:config] config)))

(reg-event-fx
  :district0x/load-smart-contracts
  interceptors
  (fn [{:keys [db]} [{:keys [:version]}]]
    {:http-xhrio
     (flatten
       (for [[key {:keys [name]}] (:smart-contracts db)]
         (contract-xhrio name
                         version
                         [:district0x/smart-contract-loaded key]
                         [::logging/error "Failed to load smart contract" {:key key :name name}
                          :district0x/load-smart-contracts])))}))

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
  (fn [{:keys [db]} [contract-key contract-json]]
    (let [;; TODO REMOVE THESE FILTERS WHEN UPDATING FRONTEND WEB3 TO 1.x
          ;; In web3 0.19.0 calling of overloaded functions doesn't work properly. It only
          ;; recognizes one signature per function name and will throw on non-matching calls.
          ;; In js, we can force correct behavior by providing explicit signature like this:
          ;; web3.eth.contract(registrarAbi).at(registrarAddr).safeTransferFrom['address,address,uint256,bytes'](x, y, z, w)
          ;; But this way of method calling is not supported by our cljs libs.
          ;; So we use a hacky fix of hiding unwanted nowhere needed functions from abis.
          ;; As a result web3 always settles on a correct signature (has no other choice).
          abi (filter #(or (not= (% "name") "safeTransferFrom")
                           (= (count (% "inputs")) 4))
                      (contract-json "abi"))
          abi (filter #(or (not= (% "name") "setAddr")
                           (= (count (% "inputs")) 2))
                      abi)
          abi (clj->js abi)
          bin (contract-json "bytecode")
          contract (get-contract db contract-key)
          contract-address (:address contract)
          new-db (-> db
                     (assoc-in [:smart-contracts contract-key :abi] abi)
                     (assoc-in [:smart-contracts contract-key :bin] bin)
                     (update-in [:smart-contracts contract-key] merge
                                (when contract-address
                                  {:instance (web3-eth/contract-at (:web3 db) abi contract-address)})))]
      (merge
        {:db new-db
         :district0x/dispatch-n [(when (all-contracts-loaded? new-db)
                                   [:district0x/smart-contracts-loaded])]}))))

(reg-empty-event-fx :district0x/smart-contracts-loaded)

(reg-event-fx
  :district0x/my-addresses-loaded
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [addresses]]
    (let [addresses (map #(string/lower-case %) addresses)
          active-address (if (contains? (set addresses) (:active-address localstorage))
                           (:active-address localstorage)
                           (first addresses))]
      (merge
        {:db (assoc db :my-addresses addresses)}
        (when-not (= (:active-address db) active-address)
          {:dispatch [:district0x/set-active-address active-address]})
        (when-not (= addresses (:my-addresses db))
          {:dispatch [:district0x/my-addresses-changed]})))))

(reg-empty-event-fx :district0x/my-addresses-changed)

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
               :on-error [::logging/error "Failed to deploy smart contract" {:contract-key contract-key
                                                                             :tx-opts tx-opts}
                          :district0x/deploy-contract]}]}})))

(reg-event-fx
  :district0x/contract-deployed
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [{:keys [:on-success :contract-key]} instance]]
    (when-let [contract-address (aget instance "address")]
      (log/info "Deployed contract at address" {:contract contract-key :address contract-address})
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
        :on-success on-address-balance-loaded
        :on-error [:district0x/blockchain-connection-error :district0x/watch-eth-balances]}})))

(reg-event-fx
  :district0x/watch-my-eth-balances
  interceptors
  (fn [{:keys [db]} [args]]
    (let [my-addresses (set (:my-addresses db))]
      (when-not (= (set/intersection (get-in db [:district0x/watch-eth-balances :addresses]) my-addresses)
                   my-addresses)
        {:dispatch [:district0x/watch-eth-balances (assoc args :addresses my-addresses)]}))))

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
        :on-success [:district0x/address-balance-loaded token-code]
        :on-error [:district0x/blockchain-connection-error :district0x/watch-token-balances]}})))

(reg-event-fx
  :district0x/load-token-balances
  interceptors
  (fn [{:keys [db]} [{:keys [:addresses :instance :token-code]}]]
    (when (seq addresses)
      {:web3-fx.blockchain/balances
       {:web3 (:web3 db)
        :addresses addresses
        :instance instance
        :on-success [:district0x/address-balance-loaded token-code]
        :on-error [:district0x/blockchain-connection-error :district0x/watch-token-balances]}})))

(reg-event-fx
  :district0x/watch-my-token-balances
  interceptors
  (fn [{:keys [db]} [args]]
    (let [addresses (:my-addresses db)]
      {:dispatch [:district0x/watch-token-balances (assoc args :addresses addresses)]})))

(reg-event-fx
  :district0x/address-balance-loaded
  interceptors
  (fn [{:keys [db]} [token balance address]]
    {:db (assoc-in db [:balances address token] (d0x-shared-utils/big-num->ether balance))}))

(reg-event-fx
  :district0x/load-conversion-rates
  interceptors
  (fn [{:keys [db]} [currencies]]
    {:http-xhrio {:method :get
                  :uri (str "https://min-api.cryptocompare.com/data/pricemulti?fsyms=ETH&tsyms="
                            (string/join "," (map name currencies))
                            "&api_key="
                            (get-in db [:config :cryptocompare-api-key]))
                  :timeout 20000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:district0x/conversion-rates-loaded]
                  :on-failure [::logging/error "Failed to load conversion rates" {:currencies currencies}
                               :district0x/load-conversion-rates]}}))

(reg-event-db
  :district0x/conversion-rates-loaded
  interceptors
  (fn [db [response]]
    ;; this fantastic service returns 200 on error and puts the error in response
    (when (= "Error" (:Response response))
      (log/error (:Message response) response :district0x/conversion-rates-loaded))
    (update db :conversion-rates merge (:ETH response))))

(reg-event-fx
  :district0x/set-active-address
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [address]]
    {:db (-> db
             (assoc :active-address address))
     :localstorage (assoc localstorage :active-address address)}))

(reg-event-fx
  :district0x/make-transaction
  [interceptors (validate-first-arg (s/keys :req-un [:transaction/form-data
                                                     :transaction/tx-opts
                                                     :transaction/contract-key
                                                     :transaction/name]
                                            :opt-un [:transaction/args-order
                                                     :transaction/contract-address
                                                     :transaction/wei-keys
                                                     :transaction/form-id
                                                     :transaction/result-href]))]
  (fn [{:keys [db]} [{:keys [:form-data :form-id :args-order :contract-key :wei-keys
                             :contract-key :contract-method :contract-address :on-success] :as props}]]
    (let [{:keys [:web3 :active-address]} db
          ;; if MetaMask is used, remove our gas limit and gas price suggestions and let it supply its own
          is-metamask (goog.object/get (goog.object/get web3 "currentProvider") "isMetaMask")
          props (update props :tx-opts (comp
                                         (partial merge {:from active-address})
                                         #(if is-metamask (dissoc % :gas :gas-price) %)))
          args (-> (d0x-shared-utils/update-multi form-data wei-keys d0x-shared-utils/eth->wei)
                   (d0x-shared-utils/map->vec args-order))]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [{:instance (if contract-address
                           (get-instance db contract-key contract-address)
                           (get-instance db contract-key))
               :method contract-method
               :args args
               :tx-opts (:tx-opts props)
               :on-success [:district0x/dispatch-n [[::logging/info "Transaction success" (merge props {:user active-address})
                                                     :district0x/make-transaction]
                                                    [:district0x/load-transaction]
                                                    [:district0x.transactions/add props]
                                                    (when on-success on-success)]]
               :on-error [::logging/error "Transaction error" (merge props {:user active-address})
                          :district0x/make-transaction]
               :on-tx-receipt [:district0x/on-tx-receipt props]}]}})))

(reg-event-fx
  :district0x/on-tx-receipt
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-tx-receipt :on-tx-receipt-n :form-data :on-error]
                       :or {on-error [:district0x.snackbar/show-transaction-error]}}
                      {:keys [:transaction-hash :gas-used] :as tx-receipt}
                      :as args]]
    (if-let [gas-limit (get-in db [:transaction-log :transactions transaction-hash :gas])]
      (let [success? (not= gas-limit gas-used)]
        (merge
          {:district0x/dispatch-n [[:district0x/transaction-receipt-loaded tx-receipt]]}
          (when (and success? on-tx-receipt)
            {:dispatch (vec (concat on-tx-receipt [form-data tx-receipt]))})
          (when (and success? on-tx-receipt-n)
            {:dispatch-n (map #(vec (concat % [form-data tx-receipt])) on-tx-receipt-n)})
          (when (and (not success?) on-error)
            {:dispatch (vec (concat on-error [form-data tx-receipt]))})))
      ;; MetaMask can load transaction only at receipt time, not earlier
      {:async-flow {:first-dispatch [:district0x/load-transaction transaction-hash]
                    :rules [{:when :seen?
                             :events [:district0x/transaction-loaded]
                             :dispatch-n [(vec (concat [:district0x/on-tx-receipt] args))]}]}})))

(reg-event-fx
  :district0x/load-transaction
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [{:f web3-eth/get-transaction
             :args [transaction-hash]
             :on-success [:district0x/transaction-loaded transaction-hash]
             :on-error [::logging/error "Failed to load transaction" {:transaction-hash transaction-hash}
                        :district0x/load-transaction]}]}}))

(reg-event-fx
  :district0x/transaction-loaded
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash {:keys [:gas :gas-price] :as transaction}]]
    (let [gas-used (get-in db [:transaction-log :transactions transaction-hash :gas-used])
          transaction (cond-> transaction
                              true (assoc :status (if (and gas gas-used)
                                                    (if (= gas-used gas)
                                                      :tx.status/failure
                                                      :tx.status/success)
                                                    :tx.status/pending))
                              true (assoc :hash transaction-hash))]
      {:dispatch [:district0x.transactions/update transaction-hash transaction]})))

(reg-event-fx
  :district0x/load-transaction-receipt
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [{:f web3-eth/get-transaction-receipt
             :args [transaction-hash]
             :on-success [:district0x/transaction-receipt-loaded]
             :on-error [::logging/error "Failed to load transaction receipt" {:transaction-hash transaction-hash}
                        :district0x/load-transaction-receipt]}]}}))

(reg-event-fx
  :district0x/transaction-receipt-loaded
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:gas-used :transaction-hash] :as tx-receipt}]]
    (when transaction-hash
      (let [gas-limit (get-in db [:transaction-log :transactions transaction-hash :gas])
            tx-receipt (assoc tx-receipt :status (if gas-limit
                                                   (if (= gas-limit gas-used)
                                                     :tx.status/failure
                                                     :tx.status/success)
                                                   :tx.status/not-loaded))]
        {:dispatch [:district0x.transactions/update transaction-hash tx-receipt]}))))

(reg-event-fx
  :district0x.transactions/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [{:keys [:tx-opts :form-id :contract-key :contract-method] :as props}
                                    tx-hash]]
    (let [tx-data (merge (select-keys props [:contract-method
                                             :contract-key
                                             :tx-opts
                                             :name
                                             :result-href])
                         {:created-on (time-coerce/to-long (t/now))
                          :hash tx-hash
                          :status :tx.status/not-loaded})
          new-db (-> db
                     (assoc-in [:transaction-log :transactions tx-hash] tx-data)
                     (update-in [:transaction-log :ids-chronological] conj tx-hash)
                     (update-in (remove nil? [:transaction-log :ids-by-form contract-key contract-method (:from tx-opts) form-id])
                                conj
                                tx-hash))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:transaction-log]))
       :dispatch [:district0x.transaction-log/set-open true tx-hash]})))

(reg-event-fx
  :district0x.transactions/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [tx-hash]]
    (let [{:keys [:contract-key :contract-method :tx-opts]} (get-in db [:transaction-log :transactions tx-hash])
          new-db (-> db
                     (medley/dissoc-in [:transaction-log :transactions tx-hash])
                     (update-in [:transaction-log :ids-chronological] (partial remove #(= tx-hash %)))
                     (update-in [:transaction-log :ids-by-form contract-key contract-method (:from tx-opts)]
                                (partial medley/map-vals (partial remove #(= tx-hash %)))))]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:transaction-log]))})))

(defn- assoc-gas-used-costs [{:keys [:gas-used :gas-price] :as transaction} usd-rate]
  (if (and gas-used gas-price)
    (let [gas-used-cost (bn/number (wei->eth (* (bn/number gas-price) gas-used)))]
      (merge transaction
             {:gas-used-cost gas-used-cost}
             (when usd-rate
               {:gas-used-cost-usd (* gas-used-cost usd-rate)})))
    transaction))

(reg-event-fx
  :district0x.transactions/update
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [transaction-hash {:keys [:gas-used :gas-price] :as transaction}]]
    (let [usd-rate (get-in db [:conversion-rates :USD])
          existing-tx (get-in db [:transaction-log :transactions transaction-hash])
          transaction (-> transaction
                          (select-keys [:block-hash :gas :gas-used :value :status :gas-price])
                          (->> (merge existing-tx))
                          (update :value bn/number)
                          (update :gas-price bn/number)
                          (assoc-gas-used-costs usd-rate))

          new-db (assoc-in db [:transaction-log :transactions transaction-hash] transaction)]
      (let [{:keys [:gas :gas-used :gas-used-cost-usd :form-data :contract-key :contract-method]} transaction]
        (when (and gas gas-used)
          (log/info (keyword contract-key contract-method)
                    {:gas-limit gas
                     :gas-used gas-used
                     :gas-used-cost-usd (to-locale-string gas-used-cost-usd 2)
                     :gas% (str (int (* (/ gas-used gas) 100)) "%")
                     :form-data form-data}
                    :district0x.transactions/update)))
      (merge
        {:db new-db
         :localstorage (merge localstorage (select-keys new-db [:transaction-log]))}))))

(reg-event-fx
  :district0x.transactions/clear
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]}]
    (let [cleared-txs (-> (select-keys district0x.ui.db/default-db [:transaction-log])
                          (update :transaction-log dissoc :settings))]
      {:db (merge db cleared-txs)
       :localstorage (merge localstorage cleared-txs)})))

(reg-event-fx
  :district0x.transaction-log.settings/set
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [key value]]
    (let [new-db (assoc-in db [:transaction-log :settings key] value)]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:transaction-log]))})))

(reg-event-fx
  :district0x.transaction-log/set-open
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db]} [open? highlighted-tx]]
    {:db (update-in db [:transaction-log :settings] merge {:open? open?
                                                           :highlighted-transaction highlighted-tx})}))

(reg-event-fx
  :district0x/load-transaction-and-receipt
  [interceptors]
  (fn [{:keys [:db]} [transaction-hash]]
    {:dispatch-n [[:district0x/load-transaction transaction-hash]
                  [:district0x/load-transaction-receipt transaction-hash]]}))

(reg-event-fx
  :district0x-emails/set-email
  [interceptors (validate-first-arg (s/keys :req [:district0x-emails/email] :opt [:district0x-emails/address]))]
  (fn [{:keys [:db]} [form-data submit-props]]
    (let [form-data (if-not (:district0x-emails/address form-data)
                      (assoc form-data :district0x-emails/address (:active-address db))
                      form-data)
          public-key (get-in db [:config :public-key])]
      {:dispatch [:district0x/make-transaction
                  (merge {:name (gstring/format "Set email %s" (:district0x-emails/email form-data))
                          :contract-key :district0x-emails
                          :contract-method :set-email
                          :form-data (update form-data :district0x-emails/email (partial encryption/encrypt-encode public-key))
                          :args-order [:district0x-emails/email]
                          :form-id (select-keys form-data [:district0x-emails/address])
                          :tx-opts {:gas 500000 :gas-price 4000000000 :from (:district0x-emails/address form-data)}}
                         submit-props)]})))

(reg-event-fx
  :district0x-emails/load
  interceptors
  (fn [{:keys [:db]} [address]]
    (when address
      (let [instance (get-instance db :district0x-emails)
            args [address]]
        {:web3-fx.contract/constant-fns
         {:fns [{:instance instance
                 :method :emails
                 :args args
                 :on-success [:district0x-emails/loaded address]
                 :on-error [::logging/error "Failed to load email address" {:contract :district0x-emails
                                                                            :method :emails
                                                                            :args args}
                            :district0x-emails/load]}]}}))))

(reg-event-fx
  :district0x-emails/loaded
  interceptors
  (fn [{:keys [:db]} [address email]]
    {:db (assoc-in db [:district0x-emails address] email)}))

(reg-event-fx
  :district0x.contract/event-watch-once
  interceptors
  (fn [{:keys [:db]} [{:keys [:contract-key :on-success :on-error] :as event-params
                       :or {on-error [::logging/error "Event watch once error" event-params :district0x.contract/event-watch-once]}}]]
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
  :district0x.server/http-get
  interceptors
  (fn [{:keys [db]} [{:keys [:http-xhrio :params :on-success :on-failure :endpoint]} :as opts]]
    (let [uri (str (url/url (:server-url db) endpoint))]
      {:http-xhrio (-> (merge
                         {:method :get
                          :timeout 20000
                          :uri uri
                          :response-format (ajax/transit-response-format)
                          :on-success on-success
                          :on-failure [::logging/error "Http GET request error" {:uri uri} :district0x.server/http-get]
                          :params params}
                         http-xhrio)
                       (update :params (partial medley/map-vals (fn [v]
                                                                  (cond
                                                                    (keyword? v) (str v)
                                                                    (sequential? v) (map str v)
                                                                    :else v)))))})))

(reg-event-fx
  :district0x.search-results/load
  interceptors
  (fn [{:keys [db]} [{:keys [:http-xhrio :params :search-results-path :append? :endpoint :on-success
                             :id-key]
                      :as opts}]]
    {:db (update-in db search-results-path merge (merge {:loading? true}
                                                        (when-not append?
                                                          {:ids []})))
     :dispatch [:district0x.server/http-get {:http-xhrio http-xhrio
                                             :endpoint endpoint
                                             :params params
                                             :on-success [:district0x.search-results/loaded opts]}]}))

(reg-event-fx
  :district0x.search-results/loaded
  interceptors
  (fn [{:keys [db]} [{:keys [:search-results-path :params :id-key :on-success :append?]}
                     {:keys [:items :total-count] :as response}]]
    (let [ids (if id-key (map #(get % id-key) items) items)
          existing-ids (if append?
                         (get-in db (concat search-results-path [:ids]))
                         [])]
      (merge
        {:db (update-in db search-results-path merge {:loading? false
                                                      :ids (concat existing-ids ids)
                                                      :total-count total-count})}
        (when on-success
          {:dispatch (vec (concat on-success [ids items response]))})))))

(reg-event-fx
  :district0x.contract/constant-fn-call
  interceptors
  (fn [{:keys [db]} [contract-key & args]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat [(get-instance db contract-key)] args [[:logging/info "Contract constant fn call success" {:contract contract-key
                                                                                                               :args args}
                                                            :district0x.contract/constant-fn-call]
                                                           [:logging/error "Contract constant fn call error" {:contract contract-key
                                                                                                              :args args}
                                                            :district0x.contract/constant-fn-call]])]}}))

(reg-event-fx
  :district0x/clear-localstorage
  interceptors
  (fn [_]
    {:localstorage nil}))

(reg-event-fx
  :district0x.log/print-db
  interceptors
  (fn [{:keys [:db]}]
    (look db)
    nil))

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
                  :action-href nil})
     :dispatch-later [{:ms (get-in db [:snackbar :timeout])
                       :dispatch [:district0x.snackbar/close]}]}))

(reg-event-fx
  :district0x.snackbar/show-transaction-error
  interceptors
  (fn [{:keys [db]}]
    {:dispatch [:district0x.snackbar/show-message "Sorry, your transaction hasn't been processed"]}))

(reg-event-fx
  :district0x.snackbar/show-message-redirect-action
  interceptors
  (fn [{:keys [db]} [{:keys [:message] :as params}]]
    {:db (update db :snackbar merge
                 {:open? true
                  :message message
                  :action-href (history/path-for (select-keys params [:route :route-params :routes]))})
     :dispatch-later [{:ms (get-in db [:snackbar :timeout])
                       :dispatch [:district0x.snackbar/close]}]}))

(reg-event-db
  :district0x.menu-drawer/set
  interceptors
  (fn [db [open?]]
    (assoc-in db [:menu-drawer :open?] open?)))

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
             [:logging/info "Unlock account success" {:user address} :district0x/unlock-account]
             [:logging/error "Unlock account failure" {:user address} :district0x/unlock-account]]]}}))

(reg-event-fx
  :district0x.location/set-query
  interceptors
  (fn [_ args]
    {:location/set-query args}))

(reg-event-fx
  :district0x.location/nav-to
  interceptors
  (fn [{:keys [:db]} [route route-params routes]]
    {:location/nav-to [route route-params routes]}))

(reg-event-fx
  :district0x.location/nav-to-url
  interceptors
  (fn [{:keys [:db]} [url]]
    {:location/nav-to-url [url]}))

(reg-event-fx
  :district0x.location/add-to-query
  interceptors
  (fn [_ [query-params]]
    {:location/add-to-query [query-params]}))

(reg-event-fx
  :district0x.location/set-query-and-nav-to
  interceptors
  (fn [{:keys [:db]} [route query-params routes]]
    {:async-flow {:first-dispatch [:district0x.location/nav-to route {} routes]
                  :rules [{:when :seen?
                           :events [:district0x.location/nav-to]
                           :dispatch [:district0x.location/set-query query-params]}]}}))

(reg-event-fx
  :district0x.window/set-focus
  interceptors
  (fn [{:keys [db]} [focused?]]
    {:db (assoc-in db [:window :focused?] focused?)}))

(reg-event-fx
  :district0x.window/resized
  interceptors
  (fn [{:keys [db]} [width]]
    {:db (assoc-in db [:window :size] (get-window-size width))}))

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
  (fn [_ [events & args]]
    {:dispatch-n (mapv #(vec (concat % args)) (remove nil? events))}))

(reg-event-fx
  :district0x/reload-my-addresses
  interceptors
  (fn [{:keys [db]}]
    (when (get-in db [:window :focused?])
      ;; Running this for longer time makes app to do little freeze every 4s. Not known why.
      ;; To make it less likely to happen, we run this only when app is focused
      {:async-flow {:first-dispatch [:district0x/load-my-addresses]
                    :rules [{:when :seen?
                             :events [:district0x/my-addresses-loaded]
                             :dispatch [:district0x/watch-my-eth-balances]}]}})))

(reg-event-fx
  :district0x/setup-address-reload-interval
  interceptors
  (fn [{:keys [db]}]
    {:dispatch-interval {:dispatch [:district0x/reload-my-addresses]
                         :ms 4000
                         :db-path [:district0x-reload-address-interval]}}))

(reg-event-fx
  :district0x.history/start
  interceptors
  (fn [_ [routes]]
    {:history/start {:routes routes}}))

(reg-fx
  :file/write
  (fn [[filename content]]
    (file-write filename content)))
