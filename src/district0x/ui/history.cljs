(ns district0x.ui.history
  (:require [bidi.bidi :as bidi]
            [clojure.string :as string]
            [district0x.ui.utils :as d0x-ui-utils]
            [medley.core :as medley]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]))

(goog-define pushroute-hosts "localhost")

(def routes (atom nil))

(def hashroutes?
  (when-not (contains? (-> pushroute-hosts
                           (string/split ",")
                           set)
                       (-> js/window
                           .-location
                           .-hostname))
    true))

(def prerender?
  (let [agent (.-userAgent (.-navigator js/window))]
    (not (= (.indexOf agent "prerenderer") -1))))

(defn path-for [{:keys [:route :route-params :routes]}]
  (let [path (medley/mapply bidi/path-for routes route route-params)]
    (if hashroutes?
      (str "#" path)
      path)))

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
