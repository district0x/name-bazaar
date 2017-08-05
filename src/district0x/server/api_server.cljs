(ns district0x.server.api-server
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils]
    [medley.core :as medley])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def express (nodejs/require "express"))
(def body-parser (nodejs/require "body-parser"))

(defonce *app* (atom nil))
(defonce *server* (atom nil))
(defonce *registered-routes* (atom {}))

(defn reg-route! [method path callback]
  (swap! *registered-routes* assoc-in [method path] callback))

(def reg-get! (partial reg-route! :get))

(defn setup-method-routes! [method]
  (doseq [[path callback] (@*registered-routes* method)]
    (condp = method
      :get (.get @*app* path callback))))

(defn send-json! [res data]
  (.json res (clj->js data)))

(defn stop! []
  (let [ch (chan)]
    (if @*server*
      (.close @*server* #(go (>! ch true)))
      (go (>! ch true)))
    ch))

(defn setup-app! []
  (reset! *app* (express))
  (.use @*app* (.urlencoded body-parser #js {:extended true}))
  (.use @*app* (.json body-parser))
  (doseq [method (keys @*registered-routes*)]
    (setup-method-routes! method)))

(defn start! [& [port]]
  (let [port (or port (aget js/process "env" "PORT"))]
    (go
      (<! (stop!))
      (setup-app!)
      (reset! *server* (.listen @*app* port (fn []
                                              (.log js/console "Server started at port" port)))))))



(defn query-params [req]
  (js->clj (aget req "query") :keywordize-keys true))

(defn restrict-limit [query]
  (update query :limit max 100))

(defn parse-keyword-value [x]
  (if (and (string? x) (string/starts-with? x ":"))
    (keyword (subs x 1))
    x))

(defn parse-keyword-values [query]
  (medley/map-vals (fn [val]
                     (if (sequential? val)
                       (mapv parse-keyword-value val)
                       (parse-keyword-value val)))
                   query))

(defn parse-boolean-values [query]
  (medley/map-vals (fn [val]
                     (condp = val
                       "true" true
                       "false" false
                       val))
                   query))

(defn parse-order-by [{:keys [:order-by-columns :order-by-dirs] :as query}]
  (-> query
    (assoc :order-by (into [] (zipmap (d0x-shared-utils/collify order-by-columns)
                                      (d0x-shared-utils/collify order-by-dirs))))
    (dissoc :order-by-columns :order-by-dirs)))

(def sanitize-query (comp restrict-limit
                          parse-order-by
                          parse-keyword-values
                          parse-boolean-values))

(def sanitized-query-params (comp sanitize-query
                                  query-params))


(comment
  (start! 6200)
  (do (.close @*server*)
      (reset! *server* nil)))