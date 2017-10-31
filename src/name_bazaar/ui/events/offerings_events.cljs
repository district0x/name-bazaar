(ns name-bazaar.ui.events.offerings-events
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs.spec.alpha :as s]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [clojure.string :as string]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address?]]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [parse-auction-offering parse-offering]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 normalize path-for get-offering-name get-offering update-search-results-params get-similar-offering-pattern debounce? try-resolving-address]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]

    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(reg-event-fx
  :buy-now-offering-factory/create-offering
  [interceptors (validate-first-arg (s/keys :req [:offering/name :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    {:dispatch [:district0x/make-transaction
                {:name (gstring/format "Create %s offering" (:offering/name form-data))
                 :contract-key :buy-now-offering-factory
                 :contract-method :create-offering
                 :form-data form-data
                 :tx-opts {:gas 320000 :gas-price default-gas-price}
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
                   :tx-opts {:gas 370000 :gas-price default-gas-price}
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
    (let [offering-name (get-offering-name db (:offering/address form-data))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Buy %s" offering-name)
                   :contract-key :buy-now-offering
                   :contract-method :buy
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :result-href (path-for :route.offerings/detail form-data)
                   :tx-opts {:gas 200000
                             :gas-price default-gas-price
                             :value (eth->wei (:offering/price form-data))}
                   :form-id (select-keys form-data [:offering/address])
                   :on-tx-receipt-n [[:district0x.snackbar/show-message
                                      (gstring/format "You successfully bought %s!" offering-name)]
                                     [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]}]})))

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
                 :tx-opts {:gas 200000 :gas-price default-gas-price}
                 :form-id (select-keys form-data [:offering/address])
                 :wei-keys #{:offering/price}
                 :on-tx-receipt-n [[:offering/set-settings-tx-receipt form-data]
                                   [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]}]}))

(reg-event-fx
  :auction-offering/bid
  [interceptors (validate-first-arg (s/keys :req [:offering/address :offering/price]))]
  (fn [{:keys [:db]} [form-data]]
    (let [offering-name (get-offering-name db (:offering/address form-data))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Bid for %s" offering-name)
                   :contract-key :auction-offering
                   :contract-method :bid
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :result-href (path-for :route.offerings/detail form-data)
                   :tx-opts {:gas 200000
                             :gas-price default-gas-price
                             :value (eth->wei (:offering/price form-data))}
                   :wei-keys #{:offering/price}
                   :form-id (select-keys form-data [:offering/address])
                   :on-tx-receipt-n [[:district0x.snackbar/show-message
                                      (gstring/format "Bid for %s was processed" offering-name)]
                                     [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]}]})))

(reg-event-fx
  :auction-offering/finalize
  [interceptors (validate-first-arg (s/keys :req [:offering/address]))]
  (fn [{:keys [:db]} [form-data]]
    (let [offering-name (get-offering-name db (:offering/address form-data))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Finalize auction %s" offering-name)
                   :contract-key :auction-offering
                   :contract-method :finalize
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :result-href (path-for :route.offerings/detail form-data)
                   :tx-opts {:gas 120000
                             :gas-price default-gas-price
                             :value (:offering/price form-data)}
                   :form-id (select-keys form-data [:offering/address])
                   :on-tx-receipt-n [[:district0x.snackbar/show-message
                                      (gstring/format "Auction %s was finalized" offering-name)]
                                     [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]}]})))

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
                                      (:auction-offering/bidder form-data)])
          formatted-returns (d0x-ui-utils/format-eth pending-returns)
          offering-name (get-offering-name db (:offering/address form-data))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Withdraw %s ETH from %s auction" formatted-returns offering-name)
                   :contract-key :auction-offering
                   :contract-method :withdraw
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :args-order [:auction-offering/bidder]
                   :result-href (path-for :route.offerings/detail form-data)
                   :tx-opts {:gas 150000 :gas-price default-gas-price}
                   :form-id (select-keys form-data [:offering/address])
                   :on-tx-receipt-n [[:district0x.snackbar/show-message
                                      (gstring/format "%s ETH was withdrawn from auction" formatted-returns)]
                                     [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]}]})))

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
                   :tx-opts {:gas 200000 :gas-price default-gas-price}
                   :on-tx-receipt-n [[:offering/set-settings-tx-receipt form-data]
                                     [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]
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
    (let [offering-name (get-offering-name db (:offering/address form-data))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Reclaim ownership from %s offering" offering-name)
                   :contract-key :buy-now-offering
                   :contract-method :reclaim-ownership
                   :form-data form-data
                   :contract-address (:offering/address form-data)
                   :result-href (path-for :route.offerings/detail form-data)
                   :form-id (select-keys form-data [:offering/address])
                   :tx-opts {:gas 200000 :gas-price default-gas-price}
                   :on-tx-receipt-n [[:district0x.snackbar/show-message
                                      (gstring/format "Ownership of %s was reclaimed" offering-name)]
                                     [:offerings/on-offering-changed {:offering (:offering/address form-data)}]]}]})))

(reg-event-fx
  :offerings/search
  interceptors
  (fn [{:keys [:db]} [opts]]
    {:dispatch [:district0x.search-results/load
                (merge
                  {:endpoint "/offerings"
                   :on-success [:offerings/load]}
                  opts)]}))

(reg-event-fx
  :offerings/load
  [interceptors (conform-args (s/cat :opts (s/? map?) :offering-addresses sequential? :rest (s/* any?)))]
  (fn [{:keys [:db]} [{:keys [:opts :offering-addresses] :as a}]]
    {:web3-fx.contract/constant-fns
     {:fns (for [offering-address offering-addresses]
             {:instance (get-instance db :buy-now-offering offering-address)
              :method :offering
              :on-success [:offerings/loaded offering-address opts]
              :on-error [:district0x.log/error]})}}))

(reg-event-fx
  :offerings/loaded
  interceptors
  (fn [{:keys [:db]} [offering-address {:keys [:load-ownership?]} offering]]
    (let [{:keys [:offering/node :offering/name :offering/label-hash :offering/auction?] :as offering}
          (parse-offering offering-address offering {:parse-dates? true :convert-to-ether? true})]
      (merge {:db (-> db
                    (update-in [:offerings offering-address] merge offering)
                    (update-in [:ens/records node] merge {:ens.record/node node
                                                          :ens.record/name name
                                                          :ens.record/label-hash label-hash}))}
             (when auction?
               {:dispatch-n [[:offerings.auction/load [offering-address]]]})
             (when load-ownership?
               {:dispatch [:offerings.ownership/load [offering-address]]})))))

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
      {:dispatch [:offerings.auction.pending-returns/load [offering-address]
                  ;; Active address should be loaded first
                  (reverse (sort-by (partial = (:active-address db)) (:my-addresses db)))]})))

(reg-event-fx
  :offerings.auction.active-address-pending-returns/load
  interceptors
  (fn [{:keys [:db]} [offering-addresses]]
    {:dispatch [:offerings.auction.pending-returns/load offering-addresses [(:active-address db)]]}))

(reg-event-fx
  :offerings.auction.pending-returns/load
  interceptors
  (fn [{:keys [:db]} [offering-addresses user-addresses]]
    {:web3-fx.contract/constant-fns
     {:fns (flatten
             (for [offering-address offering-addresses]
               (let [instance (get-instance db :auction-offering offering-address)]
                 (for [user-address user-addresses]
                   {:instance instance
                    :method :pending-returns
                    :args [user-address]
                    :on-success [:offerings.auction.pending-returns/loaded offering-address user-address]
                    :on-error [:district0x.log/error]}))))}}))

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
  (fn [{:keys [:db]} [offering-addresses]]
    (let [offerings (vals (select-keys (:offerings db) offering-addresses))
          nodes (map :offering/node offerings)
          label-hashes (->> offerings
                         (filter :offering/top-level-name?)
                         (map :offering/label-hash))]
      {:dispatch-n [[:ens.records/load nodes]
                    [:registrar.entries/load label-hashes]]})))

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
  (fn [{:keys [:db]} [{:keys [:offering]}]]
    (merge
      {:dispatch-n [[:offerings/load [offering]]
                    [:offerings.ownership/load [offering]]
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
    {:dispatch-n [[:offerings.ownership/load [address]]
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
  (fn [{old-db :db} [search-params opts]]
    (let [search-results-path [:search-results :offerings :main-search]
          search-params-path (conj search-results-path :params)
          {new-db :db new-search-params :search-params} (update-search-results-params old-db search-params-path search-params opts)]
      {:db new-db
       :dispatch-debounce {:key :offerings/search
                           :event [:offerings/search {:search-results-path search-results-path
                                                      :append? (:append? opts)
                                                      :params (d0x-shared-utils/update-multi
                                                                new-search-params
                                                                [:min-price :max-price]
                                                                d0x-shared-utils/safe-eth->wei->num)}]
                           :delay (if (debounce? (get-in old-db search-params-path)
                                                 new-search-params
                                                 [:name :min-price :max-price])
                                    300
                                    0)}})))

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
    (let [search-params (if (and (:new-owner search-params)
                                 (not (web3/address? (:new-owner search-params))))
                          (update search-params :new-owner (partial try-resolving-address db))
                          search-params)
          search-results-path [:search-results :offerings :user-purchases]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :params search-params}]})))

(reg-event-fx
  :offerings.user-offerings/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-params (if (and (:original-owner search-params)
                                 (not (web3/address? (:original-owner search-params))))
                          (update search-params :original-owner (partial try-resolving-address db))
                          search-params)
          search-results-path [:search-results :offerings :user-offerings]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)
          {:keys [:open? :finalized?]} search-params
          search-params (cond-> search-params
                          (and open? finalized?) (dissoc :finalized?)
                          (and open? (not finalized?)) (assoc :finalized? false)
                          (and (not open?) finalized?) (assoc :finalized? true)
                          true (dissoc :open?))]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :on-success [:offerings/load {:load-ownership? (not (:finalized? search-params))}]
                                     :params search-params}]})))

(reg-event-fx
  :offerings.user-bids/set-params-and-search
  interceptors
  (fn [{:keys [:db]} [search-params opts]]
    (let [search-params (if (and (:bidder search-params)
                                 (not (web3/address? (:bidder search-params))))
                          (update search-params :bidder (partial try-resolving-address db))
                          search-params)
          search-results-path [:search-results :offerings :user-bids]
          search-params-path (conj search-results-path :params)
          {:keys [:db :search-params]} (update-search-results-params db search-params-path search-params opts)
          {:keys [:winning? :outbid? :bidder]} search-params]
      {:db db
       :dispatch [:offerings/search {:search-results-path search-results-path
                                     :append? (:append? opts)
                                     :on-success
                                     [:district0x/dispatch-n [[:offerings/load]
                                                              [:offerings.auction.active-address-pending-returns/load]]]
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
    (let [search-results-path [:search-results :offerings :home-page-autocomplete]]
      {:dispatch [:offerings/search {:search-results-path search-results-path
                                     :params (merge
                                               {:name-position :any
                                                :offset 0
                                                :limit 10
                                                :node-owner? true
                                                :top-level-names? true}
                                               search-params)}]})))

(reg-event-fx
  :offerings.home-page/search
  interceptors
  (fn [{:keys [:db]}]
    (let [common-params {:limit 5
                         :node-owner? true
                         :min-end-time-now? true
                         :exclude-special-chars? true
                         :top-level-names? true}]
      {:dispatch-n [[:offerings/search {:search-results-path [:search-results :offerings :home-page-newest]
                                        :params (merge common-params
                                                       {:order-by-columns [:created-on]
                                                        :order-by-dirs [:desc]})}]
                    [:offerings/search {:search-results-path [:search-results :offerings :home-page-most-active]
                                        :params (merge common-params
                                                       {:order-by-columns [:bid-count]
                                                        :order-by-dirs [:desc]})}]
                    [:offerings/search {:search-results-path [:search-results :offerings :home-page-ending-soon]
                                        :params (merge common-params
                                                       {:order-by-columns [:end-time]
                                                        :order-by-dirs [:asc]})}]]})))

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
