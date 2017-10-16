(ns district0x.ui.history
  (:require [district0x.ui.utils :as d0x-ui-utils]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]))

(def routes (atom nil))

(defn- browser []
  (letfn [(dispatch-route [matched-route]
            (re-frame/dispatch [:district0x/set-active-page matched-route]))]
    (pushy/pushy dispatch-route #(d0x-ui-utils/match-current-location @routes %))))

(defn start! [ui-routes]
  (reset! routes ui-routes)
  (pushy/start! (browser)))

(defn get-state []
  (pushy/get-token (browser)))

(defn set-state! [token]
  (pushy/set-token! (browser) token))
