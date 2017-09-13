(ns name-bazaar.ui.events
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
    [clojure.string :as str]
    [clojure.string :as string]
    [day8.re-frame.async-flow-fx]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address?]]
    [district0x.ui.debounce-fx]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils :refer [url-query-params->form-data]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.db :refer [default-db]]
    [name-bazaar.ui.events.ens-events]
    [name-bazaar.ui.events.infinite-list-events]
    [name-bazaar.ui.events.offering-requests-events]
    [name-bazaar.ui.events.offerings-events]
    [name-bazaar.ui.events.registrar-events]
    [name-bazaar.ui.events.watched-names-events]
    [name-bazaar.ui.spec]
    [name-bazaar.ui.utils :refer [namehash sha3 name->label-hash parse-query-params get-offering-search-results get-offering-requests-search-results]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))


(def active-address-changed-forwarding {:register :active-address-changed
                                        :events #{:district0x/set-active-address}
                                        :dispatch-to [:active-page-changed]})

(defn- route->initial-effects [{:keys [:handler :route-params :query-params]} db]
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
                                          [:offerings.similar-offerings/set-params-and-search
                                           {:offering/address address}
                                           {:reset-params? true}]]}]}})

    :route.ens-record/detail
    {:dispatch-n [[:name/load-all-details (:ens.record/name route-params)]
                  [:offerings.ens-record-offerings/set-params-and-search
                   {:node (namehash (:ens.record/name route-params))}
                   {:reset-params? true}]]}

    :route/watched-names
    {:dispatch [:watched-names/load-all]}

    :route.user/purchases
    {:dispatch [:offerings.user-purchases/set-params-and-search
                {:new-owner (:user/address route-params)}
                {:reset-params? true}]}

    :route.user/my-purchases
    {:dispatch [:offerings.user-purchases/set-params-and-search
                {:new-owner (:active-address db)}
                {:reset-params? true}]
     :forward-events active-address-changed-forwarding}

    :route.user/bids
    {:dispatch [:offerings.user-bids/set-params-and-search
                {:bidder (:user/address route-params)}
                {:reset-params? true}]}

    :route.user/my-bids
    {:dispatch [:offerings.user-bids/set-params-and-search
                {:bidder (:active-address db)}
                {:reset-params? true}]
     :forward-events active-address-changed-forwarding}

    :route.user/offerings
    {:dispatch [:offerings.user-offerings/set-params-and-search
                {:original-owner (:user/address route-params)}
                {:reset-params? true}]}

    :route.user/my-offerings
    {:dispatch [:offerings.user-offerings/set-params-and-search
                {:original-owner (:active-address db)}
                {:reset-params? true}]
     :forward-events active-address-changed-forwarding}

    :route.user/my-settings
    {:dispatch [:district0x-emails/load (:active-address db)]
     :forward-events active-address-changed-forwarding}


    nil))

(reg-event-fx
  :active-page-changed
  interceptors
  (fn [{:keys [:db]}]
    (merge
      {:forward-events {:unregister :active-address-changed}}
      (route->initial-effects (:active-page db) db)
      {:district0x/dispatch [:offerings/stop-watching-all]
       :db (assoc-in db [:infinite-list :expanded-items] {})})))

(reg-event-fx
  :name/load-all-details
  interceptors
  (fn [{:keys [:db]} [name]]
    (let [node (namehash name)]
      (merge {:dispatch-n [[:ens.records/load [node] {:load-resolver? true}]
                           [:offering-requests.has-requested/load node (:my-addresses db)]]}
             (when (top-level-name? name)
               {:dispatch [:registrar.entries/load [(name->label-hash name)]]})))))

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
    (let [new-db (medley/dissoc-in db [:saved-searches saved-searches-key query-string])]
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


