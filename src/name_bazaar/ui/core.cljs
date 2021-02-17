(ns name-bazaar.ui.core
  (:require
    [cljs-time.extend]
    [cljs.spec.alpha :as s]
    [cljsjs.web3]
    [district.ui.mobile]
    [district.ui.logging]
    [district0x.ui.events]
    [district0x.ui.history :as history]
    [district0x.ui.subs]
    [district0x.ui.utils :as d0x-ui-utils :refer [prerender-user-agent?]]
    [madvas.re-frame.google-analytics-fx :as google-analytics-fx]
    [mount.core :as mount]
    [name-bazaar.ui.components.main-panel :refer [main-panel]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.config :refer [config]]
    [name-bazaar.ui.db :as ui-db]
    [name-bazaar.ui.events]
    [name-bazaar.ui.subs]
    [print.foo :include-macros true]
    [re-frame.core :refer [dispatch dispatch-sync clear-subscription-cache!]]
    [reagent.core :as r]
    [taoensso.timbre :as logging :refer-macros [info warn error]]
    [name-bazaar.ui.utils :refer [run-ignoring-errors]]))

(def debug? ^boolean (:debug? config))

(defn dev-setup []
  (when debug?
    (enable-console-print!)))

(defn mount-root []
  (run-ignoring-errors (fn [] (google-analytics-fx/set-enabled! (not debug?))))
  (clear-subscription-cache!)
  (-> (mount/with-args config)
      (mount/start #'district.ui.mobile/mobile
                   #'district.ui.logging/logging))
  (r/render [main-panel] (.getElementById js/document "app")))

(defn ^:export init []
  (s/check-asserts goog.DEBUG)
  (dev-setup)
  (run-ignoring-errors (fn [] (google-analytics-fx/set-enabled! (not debug?))))
  (dispatch-sync [:district0x/initialize
                  {:default-db name-bazaar.ui.db/default-db
                   :effects
                   (merge
                     {:async-flow {:first-dispatch [:district0x/load-smart-contracts {:version constants/contracts-version}]
                                   :rules [{:when :seen?
                                            :events (remove nil? [:district0x/smart-contracts-loaded
                                                                  (when-not prerender-user-agent?
                                                                    :district0x/my-addresses-loaded)])
                                            :dispatch-n [[:district0x/watch-my-eth-balances]
                                                         [:watch-my-addresses-changed]
                                                         (if history/hashroutes?
                                                           [:active-page-changed]
                                                           [:district0x.history/start constants/routes])]}
                                           {:when :seen?
                                            :events [:district.server.config/loaded]
                                            :dispatch-n [[:setup-update-now-interval]
                                                         [:district0x/load-conversion-rates [:USD]]]}]}
                      :forward-events {:register :active-page-changed
                                       :events #{:district0x/set-active-page}
                                       :dispatch-to [:active-page-changed]}
                      :dispatch-n [[:district.server.config/load]]}
                     (when history/hashroutes?
                       {:window/on-hashchange {:dispatch [:district0x/set-current-location-as-active-page constants/routes]}}))}])
  (mount-root))
