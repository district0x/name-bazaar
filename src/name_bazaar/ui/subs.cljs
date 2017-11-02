(ns name-bazaar.ui.subs
  (:require
    [cemerick.url :as url]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [clojure.string :as string]
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
    [name-bazaar.ui.subs.registrar-subs]
    [name-bazaar.ui.subs.watched-names-subs]
    [name-bazaar.ui.utils :refer [parse-query-params path-for try-resolving-address]]
    [re-frame.core :refer [reg-sub subscribe reg-sub-raw]]
    [reagent.ratom :refer-macros [reaction]]))

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
 :<- [:district0x/active-address]
 (fn [[root-url my-address] [_ route params]]
   (string/replace
    (str
     root-url
     (path-for route (merge
                      (when my-address
                        {:user/address (str my-address)})
                      params)))
    "#" "")))

(reg-sub-raw
 :resolved-route-params
 (fn [db p]
   (reaction
    (let [route-params @(subscribe [:district0x/route-params])]
      (update
       route-params
       :user/address (partial try-resolving-address @db))))))
