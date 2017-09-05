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
    [name-bazaar.shared.utils :refer [parse-offering parse-auction-offering parse-registrar-entry offering-version->type]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.db :refer [default-db]]
    [name-bazaar.ui.events.ens]
    [name-bazaar.ui.events.infinite-list]
    [name-bazaar.ui.events.offering-requests]
    [name-bazaar.ui.events.offerings]
    [name-bazaar.ui.events.registrar]
    [name-bazaar.ui.events.watched-names]
    [name-bazaar.ui.spec]
    [name-bazaar.ui.utils :refer [namehash sha3 parse-query-params path-for get-node-name get-offering-name get-offering auction-offering? get-offering-search-results]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(defn- route->initial-effects [{:keys [:handler :route-params :query-params]}]
  (condp = handler
    :route.offerings/search
    {:dispatch [:offerings.main-search/set-params-and-search
                (merge
                  (:params (get-offering-search-results default-db :main-search))
                  (parse-query-params query-params :route.offerings/search))
                {:clear-existing-items? true
                 :clear-existing-params? true}]}

    :route.offerings/edit
    {:dispatch [:offerings/load [(:offering/address route-params)]]}

    :route.offerings/detail
    (let [{:keys [:offering/address]} route-params]
      {:async-flow {:first-dispatch [:offerings/load [address]]
                    :rules [{:when :seen?
                             :events [:offerings/loaded]
                             :dispatch-n [[:offerings.ownership/load address]
                                          [:offerings.auction.my-addresses-pending-returns/load address]
                                          [:offerings/watch [address]]]}]}})
    nil))

(reg-event-fx
  :active-page-changed
  interceptors
  (fn [{:keys [:db]}]
    (merge
      (route->initial-effects (:active-page db))
      {:district0x/dispatch [:offerings/stop-watching-all]
       :db (assoc-in db [:infinite-list :expanded-items] {})})))

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


