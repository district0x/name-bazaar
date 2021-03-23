(ns name-bazaar.ui.subs
  (:require
    [cemerick.url :as url]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [clojure.string :as string]
    [district.ui.mobile.subs :as mobile-subs]
    [district0x.shared.utils :as d0x-shared-utils :refer [empty-address? zero-address?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [time-remaining time-biggest-unit format-time-duration-unit]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.shared.utils :refer [emergency-state-new-owner]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.subs.ens-subs]
    [name-bazaar.ui.subs.infinite-list-subs]
    [name-bazaar.ui.subs.offering-requests-subs]
    [name-bazaar.ui.subs.offerings-subs]
    [name-bazaar.ui.subs.public-resolver-subs]
    [name-bazaar.ui.subs.registrar-subs]
    [name-bazaar.ui.subs.reverse-registrar-subs]
    [name-bazaar.ui.subs.watched-names-subs]
    [name-bazaar.ui.utils :refer [parse-query-params path-for reverse-resolve-address strip-root-registrar-suffix]]
    [re-frame.core :refer [reg-sub subscribe reg-sub-raw]]
    [reagent.ratom :refer-macros [reaction]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(reg-sub
  :now
  (fn [db]
    (:now db)))

(reg-sub
  :root-url
  (fn [db]
    (:root-url db)))

(reg-sub
  :saved-searches
  (fn [db [_ saved-searches-key]]
    (get-in db [:saved-searches saved-searches-key])))

(reg-sub
  :saved-search/active?
  (fn [[_ saved-searches-key]]
    [(subscribe [:saved-searches saved-searches-key])
     (subscribe [:district0x/query-string])])
  (fn [[saved-searches query-string]]
    (boolean (get saved-searches query-string))))

(reg-sub
  :search-results
  (fn [db [_ db-path]]
    (get-in db (concat [:search-results] db-path))))

(reg-sub
  :page-share-url
  :<- [:root-url]
  (fn [root-url [_ route params]]
    (let [params (update params :user/address strip-root-registrar-suffix)]
      (when (:user/address params)
        (string/replace
          (str
            root-url
            (path-for route params))
          "#" "")))))

(reg-sub
  :reverse-resolved-address
  :<- [:public-resolver/reverse-records]
  (fn [reverse-records [_ address]]
    (or (reverse-resolve-address reverse-records address) address)))

(reg-sub
  :resolved-active-address
  :<- [:district0x/active-address]
  :<- [:public-resolver/reverse-records]
  (fn [[active-address reverse-records]]
    (or (reverse-resolve-address reverse-records active-address) active-address)))

(reg-sub
  :resolved-route-params
  :<- [:district0x/route-params]
  :<- [:public-resolver/reverse-records]
  (fn [[route-params reverse-records]]
    (cond-> route-params
            (:user/address route-params)
            (update :user/address #(or (reverse-resolve-address reverse-records (:user/address route-params))
                                       (:user/address route-params))))))

(reg-sub
  :transfer-ownership/tx-pending?
  (fn [[node label top-level-name?]]
    (if top-level-name?
      [(subscribe [:eth-registrar.transfer/tx-pending? label])]
      [(subscribe [:ens.set-owner/tx-pending? node])]))
  first)

;; TODO: move into district.ui.mobile repo
(reg-sub
  ::mobile-subs/coinbase-appstore-link
  :<- [::mobile-subs/android?]
  :<- [::mobile-subs/ios?]
  (fn [[android? ios?]]
    (cond
      android? (:android-mobile-link constants/coinbase)
      ios? (:ios-mobile-link constants/coinbase)
      :else (:main-mobile-link constants/coinbase))))
