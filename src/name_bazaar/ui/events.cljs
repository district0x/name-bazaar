(ns name-bazaar.ui.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [bignumber.core :as bn]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs-web3.utils :as web3-utils]
    [cljs.spec.alpha :as s]
    [clojure.data :as data]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.string :as string]
    [day8.re-frame.async-flow-fx]
    [district.ui.logging.events :as logging]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address?]]
    [district0x.ui.debounce-fx]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx all-contracts-loaded?]]
    [district0x.ui.history :as history]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils :refer [truncate]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [top-level-name? name-label]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.db :refer [default-db]]
    [name-bazaar.ui.events.ens-events]
    [name-bazaar.ui.events.infinite-list-events]
    [name-bazaar.ui.events.offering-requests-events]
    [name-bazaar.ui.events.offerings-events]
    [name-bazaar.ui.events.public-resolver-events]
    [name-bazaar.ui.events.registrar-events]
    [name-bazaar.ui.events.reverse-registrar-events]
    [name-bazaar.ui.events.watched-names-events]
    [name-bazaar.ui.spec]
    [name-bazaar.ui.utils :as nb-ui-utils :refer [reverse-record-node namehash sha3 name->label-hash parse-query-params get-offering-search-results get-offering-requests-search-results ensure-registrar-root-suffix path-for]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [taoensso.timbre :as log]
    ))

(def active-address-changed-forwarding {:register :active-address-changed
                                        :events #{:district0x/set-active-address}
                                        :dispatch-to [:active-page-changed]})

(defn- reverse-resolved-route-address? [address event]
  (if (web3/address? address)
    true
    (and (= (first event) :public-resolver.addr/loaded)
         (= (second event) (namehash (ensure-registrar-root-suffix address))))))

(defn- route->initial-effects [{:keys [:handler :route-params :query-params]} db]
  (log/info "Handling active page change" {:handler handler} ::route->initial-effects)
  (condp = handler
    :route.offerings/search
    {:dispatch [:offerings.main-search/set-params-and-search
                (parse-query-params query-params :route.offerings/search)
                {:reset-params? true}]}

    :route.offering-requests/search
    {:dispatch [:offering-requests.main-search/set-params-and-search
                (parse-query-params query-params :route.offering-requests/search)
                {:reset-params? true}]}

    :route.offerings/edit
    {:async-flow {:first-dispatch [:offerings/load [(:offering/address route-params)]]
                  :rules [{:when :seen?
                           :events [:offerings/loaded]
                           :dispatch-n [[:offerings.ownership/load [(:offering/address route-params)]]]}]}}

    :route.offerings/detail
    (let [{:keys [:offering/address]} route-params]
      {:async-flow {:first-dispatch [:offerings/load [address]]
                    :rules [{:when :seen?
                             :events [:offerings/loaded]
                             :dispatch-n [[:offerings.ownership/load [address]]
                                          [:offerings.auction.my-addresses-pending-returns/load address]
                                          [:offerings/watch [address]]
                                          [:offerings/resolve-addresses address]
                                          [:offerings.similar-offerings/set-params-and-search
                                           {:offering/address address}
                                           {:reset-params? true}]]}]}})

    :route.offerings/create
    {:dispatch [:name.ownership/load (:name query-params)]}

    :route.ens-record/detail
    {:dispatch-n [[:name.all-details/load (:ens.record/name route-params)]
                  [:offerings.ens-record-offerings/set-params-and-search
                   {:node (namehash (:ens.record/name route-params))}
                   {:reset-params? true}]]}

    :route/watched-names
    {:dispatch [:watched-names/load-all]}

    :route.user/purchases
    {:async-flow {:first-dispatch [:resolve-route-user-address]
                  :rules [{:when :seen?
                           :events [(partial reverse-resolved-route-address? (:user/address route-params))]
                           :halt? true
                           :dispatch [:offerings.user-purchases/set-params-and-search
                                      {:new-owner (:user/address route-params)}
                                      {:reset-params? true}]}]}}

    :route.user/my-purchases
    {:dispatch [:offerings.user-purchases/set-params-and-search
                {:new-owner (:active-address db)}
                {:reset-params? true}]
     :forward-events active-address-changed-forwarding}

    :route.user/bids
    {:async-flow {:first-dispatch [:resolve-route-user-address]
                  :rules [{:when :seen?
                           :events [(partial reverse-resolved-route-address? (:user/address route-params))]
                           :halt? true
                           :dispatch [:offerings.user-bids/set-params-and-search
                                      {:bidder (:user/address route-params)}
                                      {:reset-params? true}]}]}}

    :route.user/my-bids
    {:dispatch [:offerings.user-bids/set-params-and-search
                {:bidder (:active-address db)}
                {:reset-params? true}]
     :forward-events active-address-changed-forwarding}

    :route.user/offerings
    {:async-flow {:first-dispatch [:resolve-route-user-address]
                  :rules [{:when :seen?
                           :events [(partial reverse-resolved-route-address? (:user/address route-params))]
                           :halt? true
                           :dispatch [:offerings.user-offerings/set-params-and-search
                                      {:original-owner (:user/address route-params)}
                                      {:reset-params? true}]}]}}

    :route.user/my-offerings
    {:dispatch [:offerings.user-offerings/set-params-and-search
                {:original-owner (:active-address db)}
                {:reset-params? true}]
     :forward-events active-address-changed-forwarding}

    :route.user/manage-names
    {:dispatch [:name.ownership/load (:name query-params)]}

    :route.user/my-settings
    {:dispatch [:district0x-emails/load (:active-address db)]
     :forward-events active-address-changed-forwarding}

    :route/home
    {:dispatch-n [[:offerings.home-page/search]
                  [:offerings.total-count/load]]}

    nil))

(reg-event-fx
  :active-page-changed
  interceptors
  (fn [{:keys [:db]}]
    (let [active-page (:active-page db)]
      (log/info "Active page changed" active-page ::active-page-changed)
      (merge
        {:forward-events {:unregister :active-address-changed}}
        (route->initial-effects active-page db)
        {:district0x/dispatch [:offerings/stop-watching-all]
         :db (assoc-in db [:infinite-list :expanded-items] {})}))))

(reg-event-fx
  :name.ownership/load
  interceptors
  (fn [{:keys [:db]} [name]]
    (when (seq name)
      (let [node (namehash name)]
        (merge {:dispatch-n [[:ens.records/load [node]]]}
               (when (top-level-name? name)
                 {:dispatch [:name-bazaar-registrar.registrations/load [(name->label-hash name)]]}))))))

(reg-event-fx
  :name.all-details/load
  interceptors
  (fn [{:keys [:db]} [name]]
    (let [node (namehash name)]
      (merge {:dispatch-n [[:offering-requests.has-requested/load node (:my-addresses db)]]
              :async-flow {:first-dispatch [:ens.records/load [node] {:load-resolver? true}]
                           :rules [{:when :seen?
                                    :events [:ens.records.owner/loaded]
                                    :halt? true
                                    :dispatch [:ens.records.owner/resolve node]}]}}
             (when (top-level-name? name)
               {:dispatch [:name-bazaar-registrar.registrations/load [(name->label-hash name)]]})))))

(reg-event-fx
  :name/transfer-ownership
  interceptors
  (fn [{:keys [:db]} [name owner]]
    {:dispatch-n
      [[:ens/set-owner {:ens.record/name name
                        :ens.record/owner owner}]
       (if (top-level-name? name)
         [:name-bazaar-registrar/transfer {:ens.record/label (name-label name)
                                           :ens.record/owner owner}
          {:result-href (path-for :route.ens-record/detail {:ens.record/name name})
           :on-tx-receipt-n [[:ens.records/load [(namehash name)]
                             {:load-resolver? true}]
                             [:district0x.snackbar/show-message
                              (gstring/format "Ownership of %s was transferred to %s"
                                              name
                                              (truncate owner 10))]]}])]}))

(reg-event-fx
  :saved-searches/add
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [saved-searches-key query-string saved-search-name]]
    (let [new-db (assoc-in db [:saved-searches saved-searches-key query-string] saved-search-name)]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:saved-searches]))})))

(reg-event-fx
  :saved-searches/remove
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [:db :localstorage]} [saved-searches-key query-string]]
    (let [new-db (update-in db [:saved-searches saved-searches-key] dissoc query-string)]
      {:db new-db
       :localstorage (merge localstorage (select-keys new-db [:saved-searches]))})))

(reg-event-fx
  :update-now
  interceptors
  (fn [{:keys [db]}]
    {:db (assoc db :now (t/now))}))

(reg-event-fx
  :setup-update-now-interval
  interceptors
  (fn [{:keys [db]}]
    {:dispatch-interval {:dispatch [:update-now]
                         :ms 1000
                         :db-path [:update-now-interval]}}))

(reg-event-fx
  :resolve-route-user-address
  interceptors
  (fn [{:keys [db]}]
    (let [name-or-addr (get-in db [:active-page :route-params :user/address])]
      (log/info "Trying address resolution" {:name-or-addr name-or-addr} ::resolve-route-user-address)
      (if (web3/address? name-or-addr)
        {:dispatch [:public-resolver.name/load name-or-addr]}
        {:dispatch [:public-resolver.addr/load (namehash (ensure-registrar-root-suffix name-or-addr))]}))))

(reg-event-fx
  :resolve-my-addresses
  interceptors
  (fn [{:keys [db]}]
    (let [addrs (get-in db [:my-addresses])]
      (log/info "Resolving address" {:address addrs} ::resolve-my-addresses)
      {:db db
       :dispatch-n (conj
                     (map #(vec [:public-resolver.name/load %]) addrs)
                     [:ens.records.resolver/load (map reverse-record-node addrs)])})))

(reg-event-fx
  :watch-my-addresses-changed
  interceptors
  (fn []
    {:dispatch [:resolve-my-addresses]
     :forward-events
     {:register :my-addresess-changed-fwd
      :events #{:district0x/my-addresses-changed}
      :dispatch-to [:resolve-my-addresses]}}))
