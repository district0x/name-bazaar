(ns district0x.ui.location-fx
  (:require
    [bidi.bidi :as bidi]
    [cemerick.url :as url]
    [cljs.spec.alpha :as s]
    [district0x.ui.history :as history]
    [district0x.ui.utils :as d0x-ui-utils]
    [goog.events :as events]
    [medley.core :as medley]
    [re-frame.core :as re-frame :refer [reg-fx]]))

(defn set-location-hash! [s]
  (set! (.-hash js/location) s))

(defn set-history! [s]
  (history/set-state! s)
  (re-frame/dispatch [:district0x/set-active-page (d0x-ui-utils/match-current-location @history/routes (history/get-state))]))

(defn nav-to! [route route-params routes]
  (let [path (medley/mapply d0x-ui-utils/path-for routes route route-params)]
    (if (d0x-ui-utils/hashroutes?)
      (set-location-hash! path)
      (set-history! path))))

(defn set-location-query! [query-params]
  (let [url-query (cond
                    (map? query-params) (when-let [query (url/map->query query-params)]
                                          (str "?" query))
                    (string? query-params) (when (seq query-params)
                                             (str "?" query-params)))]    
    (if (d0x-ui-utils/hashroutes?)
      (set-location-hash! (str (d0x-ui-utils/current-location-hash) url-query))
      (let [{:keys [:path]} (url/url (history/get-state))]
        (set-history! (str path url-query))))))

(defn add-to-location-query! [query-params]
  (let [current-query (:query (d0x-ui-utils/current-url))
        new-query (merge current-query (->> query-params
                                         (medley/remove-keys nil?)
                                         (medley/map-keys name)))]
    (set-location-query! new-query)))

(reg-fx
  :location/nav-to
  (fn [[route route-params routes]]
    (nav-to! route route-params routes)))

(reg-fx
  :location/add-to-query
  (fn [[query-params]]
    (add-to-location-query! query-params)))

(reg-fx
  :location/set-query
  (fn [[query-params]]
    (set-location-query! query-params)))
