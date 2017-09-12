(ns name-bazaar.ui.events.offerings-events
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address?]]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [parse-auction-offering parse-offering]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 normalize path-for get-offering-name get-offering update-search-results-params get-similar-offering-pattern]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [district0x.ui.utils :as d0x-ui-utils]
    [clojure.string :as string]))

(reg-event-fx
  :buy-now-offering-factory/create-offering
  [interceptors (validate-first-arg (s/keys :req [:offering/name :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Create %s offering" (:offering/name form-data))
                 :contract-key :buy-now-offering-factory
                 :contract-method :create-offering
                 :form-data form-data
                 :tx-opts {:gas 600000 :gas-price default-gas-price}
                 :result-href (path-for :route.ens-record/detail {:ens.record/name (:offering/name form-data)})
                 :args-order [:offering/name
                              :offering/price]
                 :wei-keys #{:offering/price}
                 :on-success [:offering/create-offering-sent form-data (:active-address db)]}]}))

(reg-event-fx
  :auction-offering-factory/create-offering
  [interceptors (validate-first-arg (s/keys :req [:offering/name
                                                  :offering/price
                                                  :auction-offering/end-time
                                                  :auction-offering/extension-duration
                                                  :auction-offering/min-bid-increase]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (-> form-data
                      (update :offering/name normalize)
                      (update :auction-offering/end-time to-epoch))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Create %s auction" (:offering/name form-data))
                   :contract-key :auction-offering-factory
                   :contract-method :create-offering
                   :form-data form-data
                   :tx-opts {:gas 800000 :gas-price default-gas-price}
                   :result-href (path-for :route.ens-record/detail {:ens.record/name (:offering/name form-data)})
                   :args-order [:offering/name
                                :offering/price
                                :auction-offering/end-time
                                :auction-offering/extension-duration
                                :auction-offering/min-bid-increase]
                   :wei-keys #{:offering/price :auction-offering/min-bid-increase}
                   :on-success [:offering/create-offering-sent form-data (:active-address db)]}]})))

(reg-event-fx
  :offering/create-offering-sent
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/name] :as offering} sender-address]]
    {:dispatch [:district0x.contract/event-watch-once {:contract-key :offering-registry
                                                       :event-name :on-offering-added
                                                       :event-filter-opts {:node (namehash name)
                                                                           :owner sender-address}
                                                       :blockchain-filter-opts "latest"
                                                       :on-success [:offering/my-offering-added offering]
                                                       :on-error [:district0x.log/error]}]}))

(reg-event-fx
  :offering/my-offering-added
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/name]} {:keys [:offering :owner :node]}]]
    {:dispatch [:district0x.snackbar/show-message-redirect-action
                {:message (str "Offering for " name " is ready!")
                 :route :route.offerings/detail
                 :route-params {:offering/address offering}
                 :routes constants/routes}]}))

(reg-event-fx
  :buy-now-offering/buy
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Buy %s" (get-offering-name db (:offering/address form-data)))
                 :contract-key :buy-now-offering
                 :contract-method :buy
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :result-href (path-for :route.offerings/detail form-data)
                 :tx-opts {:gas 100000
                           :gas-price default-gas-price
                           :value (eth->wei (:offering/price form-data))}
                 :form-id (select-keys form-data [:offering/address])}]}))

(reg-event-fx
  :buy-now-offering/set-settings
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Edit %s offering" (get-offering-name db (:offering/address form-data)))
                 :contract-key :buy-now-offering
                 :contract-method :set-settings
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :args-order [:offering/price]
                 :result-href (path-for :route.offerings/detail form-data)
                 :tx-opts {:gas 250000 :gas-price default-gas-price}
                 :form-id (select-keys form-data [:offering/address])
                 :wei-keys #{:offering/price}
                 :on-tx-receipt [:offering/set-settings-tx-receipt form-data]}]}))

(reg-event-fx
  :auction-offering/bid
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Bid for %s" (get-offering-name db (:offering/address form-data)))
                 :contract-key :auction-offering
                 :contract-method :bid
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :result-href (path-for :route.offerings/detail form-data)
                 :tx-opts {:gas 100000
                           :gas-price default-gas-price
                           :value (eth->wei (:offering/price form-data))}
                 :wei-keys #{:offering/price}
                 :form-id (select-keys form-data [:offering/address])}]}))

(reg-event-fx
  :auction-offering/finalize
  [interceptors (validate-first-arg (s/keys :req [:offering/address :auction-offering/transfer-price?]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Finalize auction %s" (get-offering-name db (:offering/address form-data)))
                 :contract-key :auction-offering
                 :contract-method :finalize
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :args-order [:auction-offering/transfer-price?]
                 :result-href (path-for :route.offerings/detail form-data)
                 :tx-opts {:gas 120000
                           :gas-price default-gas-price
                           :value (:offering/price form-data)}
                 :form-id (select-keys form-data [:offering/address])}]}))

(reg-event-fx
  :auction-offering/withdraw
  [interceptors (validate-first-arg (s/keys :req [:offering/address] :opt [:auction-offering/bidder]))]
  (fn [{:keys [:db]} [{:keys [:offering/address] :as form-data}]]
    (let [form-data (if-not (:auction-offering/bidder form-data)
                      (assoc form-data :auction-offering/bidder (:active-address db))
                      form-data)
          pending-returns (get-in db [:offerings
                                      address
                                      :auction-offering/pending-returns
                                      (:auction-offering/bidder form-data)])]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Withdraw %s ETH from %s auction"
                                         (d0x-ui-utils/format-eth pending-returns)
                                         (get-offering-name db (:offering/address form-data)))
                   :contract-key :auction-offering
                   :contract-method :withdraw
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :args-order [:auction-offering/bidder]
                   :result-href (path-for :route.offerings/detail form-data)
                   :tx-opts {:gas 150000 :gas-price default-gas-price}
                   :form-id (select-keys form-data [:offering/address])}]})))

(reg-event-fx
  :auction-offering/set-settings
  [interceptors (validate-first-arg (s/keys :req [:offering/address
                                                  :offering/price
                                                  :auction-offering/end-time
                                                  :auction-offering/extension-duration
                                                  :auction-offering/min-bid-increase]))]
  (fn [{:keys [:db]} [form-data]]
    (let [form-data (update form-data :auction-offering/end-time to-epoch)]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Edit %s auction" (get-offering-name db (:offering/address form-data)))
                   :contract-key :auction-offering
                   :contract-method :set-settings
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :result-href (path-for :route.offerings/detail form-data)
                   :args-order [:offering/price
                                :auction-offering/end-time
                                :auction-offering/extension-duration
                                :auction-offering/min-bid-increase]
                   :form-id (select-keys form-data [:offering/address])
                   :tx-opts {:gas 1000000 :gas-price default-gas-price}
                   :on-tx-receipt [:offering/set-settings-tx-receipt form-data]
                   :wei-keys #{:offering/price :auction-offering/min-bid-increase}}]})))

(reg-event-fx
  :offering/set-settings-tx-receipt
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/name :offering/address]}]]
    {:dispatch [:district0x.snackbar/show-message-redirect-action
                {:message (str "Offering for " name " was updated!")
                 :route :route.offerings/detail
                 :route-params {:offering/address address}
                 :routes constants/routes}]}))

(reg-event-fx
  :offering/reclaim-ownership
  [interceptors (validate-first-arg (s/keys :req [:offering/address]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Reclaim ownership from %s offering"
                                       (get-offering-name db (:offering/address form-data)))
                 :contract-key :buy-now-offering
                 :contract-method :reclaim-ownership
                 :form-data form-data
                 :contract-address (:offering/address form-data)
                 :result-href (path-for :route.offerings/detail form-data)
                 :form-id (select-keys form-data [:offering/address])
                 :tx-opts {:gas 200000 :gas-price default-gas-price}}]}))

(reg-event-fx
  :offerings/search
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:endpoint "/offerings"
                   :on-success [:offerings/load]}
                  (assoc-in opts [:params :total-count?] true))]}))

(reg-event-fx
  :offerings/load
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (for [offering-address offering-addresses]
             {:instance (get-instance db :buy-now-offering offering-address)
              :method :offering
              :on-success [:offerings/loaded offering-address]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :offerings/loaded
  interceptors
  (fn [{:keys [:db]} [offering-address offering]]
    (let [{:keys [:offering/node :offering/name :offering/label-hash :offering/auction?] :as offering}
          (parse-offering offering-address offering {:parse-dates? true :convert-to-ether? true})]
      (merge {:db (-> db
                    (update-in [:offerings offering-address] merge offering)
                    (update-in [:ens/records node] merge {:ens.record/node node
                                                          :ens.record/name name
                                                          :ens.record/label-hash label-hash}))}
             (when auction?
               {:dispatch-n [[:offerings.auction/load [offering-address]]]})))))

(reg-event-fx
  :offerings.auction/load
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (for [offering-address offering-addresses]
             {:instance (get-instance db :auction-offering offering-address)
              :method :auction-offering
              :on-success [:offerings.auction/loaded offering-address]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :offerings.auction/loaded
  interceptors
  (fn [{:keys [:db]} [offering-address auction-offering]]
    (let [offering (parse-auction-offering auction-offering {:parse-dates? true :convert-to-ether? true})]
      {:db (update-in db [:offerings offering-address] merge offering)})))

(reg-event-fx
  :offerings.auction.my-addresses-pending-returns/load
  interceptors
  (fn [{:keys [:db]} [offering-address]]
    (when (:offering/auction? (get-offering db offering-address))
      {:dispatch [:offerings.auction.pending-returns/load offering-address
                  ;; Active address should be loaded first
                  (reverse (sort-by (partial = (:active-address db)) (:my-addresses db)))]})))

(reg-event-fx
  :offerings.auction.pending-returns/load
  interceptors
  (fn [{:keys [:db]} [offering-address addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (for [address addresses]
             {:instance (get-instance db :auction-offering offering-address)
              :method :pending-returns
              :args [address]
              :on-success [:offerings.auction.pending-returns/loaded offering-address address]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :offerings.auction.pending-returns/loaded
  interceptors
  (fn [{:keys [:db]} [offering-address address pending-returns]]
    {:db (assoc-in db
                   [:offerings offering-address :auction-offering/pending-returns address]
                   (d0x-shared-utils/wei->eth->num pending-returns))}))


(reg-event-fx
  :offerings.ownership/load
  interceptors
  (fn [{:keys [:db]} [offering-address]]
    (let [{:keys [:offering/label-hash :offering/node :offering/top-level-name?]} (get-offering db offering-address)]
      (merge
        {:dispatch-n [[:ens.records/load [node]]]}
        (when top-level-name?
          {:dispatch [:registrar.entry/load label-hash]})))))

(reg-event-fx
  :offerings/watch
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    {:web3-fx.contract/events
     {:db-path [:offerings/watched]
      :events (for [address offering-addresses]
                {:instance (get-instance db :offering-registry)
                 :event-id address
                 :event-name :on-offering-changed
                 :event-filter-opts {:offering address}
                 :blockchain-filter-opts "latest"
                 :on-success [:offerings/on-offering-changed]
                 :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :offerings/on-offering-changed
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering :version]}]]
    (merge
      {:dispatch-n [[:offerings/load [offering]]
                    [:offerings.ownership/load offering]
                    [:offerings.auction.my-addresses-pending-returns/load offering]]})))

(reg-event-fx
  :offerings/stop-watching
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    (when (seq offering-addresses)
      {:web3-fx.contract/events-stop-watching
       {:db-path [:offerings/watched]
        :event-ids offering-addresses}})))

(reg-event-fx
  :offerings/stop-watching-all
  interceptors
  (fn [{:keys [:db]}]
    {:dispatch [:offerings/stop-watching (keys (:offerings/watched db))]}))

(reg-event-fx
  :offerings.list-item/expanded
  interceptors
  (fn [{:keys [:db]} [{:keys [:offering/address]}]]
    {:dispatch-n [[:offerings.ownership/load address]
                  [:offerings.auction.my-addresses-pending-returns/load address]
                  [:offerings/watch [address]]
                  [:offerings/load [address]]]}))

(reg-event-fx
  :offerings.list-item/collapsed
  interceptors
  (fn [{:keys [:db]} [offering]]
    {:dispatch [:offerings/stop-watching [(:offering/address offering)]]}))

(reg-event-fx
  :offerings.search-params-drawer/set
  interceptors
  (fn [{:keys [:db]} [open?]]
    {:db (assoc-in db [:offerings-main-search-drawer :open?] open?)}))

(reg-event-fx
  :offerings.user-bids/search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    {:dispatch [:offerings/search
                (merge
                  {:search-results-path [:search-results :offerings :user-bids]
                   :append? true
                   :params search-params}
                  opts)]}))

(reg-event-fx
  :offerings.main-search/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-results-path [:search-results :offerings :main-search]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :params (d0x-shared-utils/update-multi
                                               search-params
                                               [:min-price :max-price]
                                               d0x-shared-utils/safe-eth->wei->num)}]})))

(reg-event-fx
  :offerings.ens-record-offerings/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-results-path [:search-results :offerings :ens-record-offerings]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :params search-params}]})))

(reg-event-fx
  :offerings.similar-offerings/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-results-path [:search-results :offerings :similar-offerings]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)
          offering (get-offering db (:offering/address search-params))]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :params (-> search-params
                                               (assoc :name (get-similar-offering-pattern offering))
                                               (assoc :exclude-node (:offering/node offering))
                                               (dissoc :offering/address search-params))}]})))

(reg-event-fx
  :offerings.user-purchases/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-results-path [:search-results :offerings :user-purchases]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :params search-params}]})))

(reg-event-fx
  :offerings.user-bids/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-results-path [:search-results :offerings :user-bids]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)
          {:keys [:winning? :outbid? :bidder]} search-params]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :params (cond-> search-params
                                               (and winning? (not outbid?))
                                               (assoc :winning-bidder bidder)

                                               (and outbid? (not winning?))
                                               (assoc :exclude-winning-bidder bidder)

                                               true
                                               (dissoc :winning? :outbid?))}]})))

(reg-event-fx
  :offerings.home-page-autocomplete/search
  interceptors
  (fn [{:keys [:db]} [search-params]]
    {:dispatch [:offerings/search {:search-results-path [:search-results :offerings :home-page-autocomplete]
                                   :params search-params}]}))

(reg-event-fx
  :offerings.saved-searches/add
  interceptors
  (fn [{:keys [:db]} [query-string saved-search-name]]
    {:dispatch [:saved-searches/add :offerings-search query-string saved-search-name]}))

(reg-event-fx
  :offerings.saved-searches/remove
  interceptors
  (fn [{:keys [:db]} [query-string]]
    {:dispatch [:saved-searches/remove :offerings-search query-string]}))