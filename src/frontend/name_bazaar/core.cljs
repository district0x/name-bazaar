(ns name-bazaar.core
  (:require
    [cljs-time.extend]
    [cljs.spec.alpha :as s]
    [cljsjs.material-ui]
    [cljsjs.react-flexbox-grid]
    [cljsjs.web3]
    [district0x.events]
    [district0x.subs]
    [madvas.re-frame.google-analytics-fx :as google-analytics-fx]
    [name-bazaar.components.main-panel :refer [main-panel]]
    [name-bazaar.constants :as constants]
    [name-bazaar.db]
    [name-bazaar.events]
    [name-bazaar.subs]
    [print.foo :include-macros true]
    [re-frame.core :refer [dispatch dispatch-sync clear-subscription-cache!]]
    [reagent.core :as r]))

(defn mount-root []
  (s/check-asserts goog.DEBUG)
  (google-analytics-fx/set-enabled! (not goog.DEBUG))
  (clear-subscription-cache!)
  ;(.clear js/console)
  (r/render [main-panel] (.getElementById js/document "app")))

(defn ^:export init []
  (s/check-asserts goog.DEBUG)
  (google-analytics-fx/set-enabled! (not goog.DEBUG))
  (dispatch-sync [:district0x/initialize
                  {:default-db name-bazaar.db/default-db
                   :effects
                   {:async-flow {:first-dispatch [:district0x/load-smart-contracts
                                                  {:version constants/contracts-version}]}}}])
  (mount-root))

