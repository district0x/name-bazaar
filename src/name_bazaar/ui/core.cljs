(ns name-bazaar.ui.core
  (:require
    [cljs-time.extend]
    [cljs.spec.alpha :as s]
    [cljsjs.web3]
    [district0x.ui.events]
    [district0x.ui.subs]
    [madvas.re-frame.google-analytics-fx :as google-analytics-fx]
    [name-bazaar.ui.components.main-panel :refer [main-panel]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.db]
    [name-bazaar.ui.events]
    [name-bazaar.ui.subs]
    [print.foo :include-macros true]
    [re-frame.core :refer [dispatch dispatch-sync clear-subscription-cache!]]
    [re-frisk.core :refer [enable-re-frisk!]]
    [reagent.core :as r]
    [district0x.ui.utils :as d0x-ui-utils]))

(def debug?
  ^boolean js/goog.DEBUG)

(defn dev-setup []
  (enable-console-print!)
  (when debug?
    (enable-re-frisk!)))

(defn mount-root []
  (s/check-asserts goog.DEBUG)
  (google-analytics-fx/set-enabled! (not debug?))
  (clear-subscription-cache!)
  ;; (.clear js/console)
  (r/render [main-panel] (.getElementById js/document "app")))

(defn ^:export init []
  (s/check-asserts goog.DEBUG)
  (dev-setup)
  (google-analytics-fx/set-enabled! (not debug?))
  (dispatch-sync [:district0x/initialize
                  {:default-db name-bazaar.ui.db/default-db
                   :effects
                   {:async-flow {:first-dispatch [:district0x/load-smart-contracts {:version constants/contracts-version}]
                                 :rules [{:when :seen?
                                          :events [:district0x/smart-contracts-loaded :district0x/my-addresses-loaded]
                                          :dispatch-n [[:district0x/watch-my-eth-balances]
                                                       [:active-page-changed]]}]}
                    :forward-events {:register :active-page-changed
                                     :events #{:district0x/set-active-page}
                                     :dispatch-to [:active-page-changed]}
                    :dispatch-n [[:setup-update-now-interval]
                                 [:district0x/load-conversion-rates [:USD]]
                                 [:district0x.config/load]]}}])
  (set! (.-onhashchange js/window)
        #(dispatch [:district0x/set-active-page (d0x-ui-utils/match-current-location constants/routes)]))
  (mount-root))
