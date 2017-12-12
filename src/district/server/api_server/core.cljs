(ns district.server.api-server.core
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [cognitect.transit :as transit]
    [district.server.api-server.middleware.defaults :as middleware]
    [district.server.config.core :refer [config]]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)

(defstate api-server
  :start (start (merge (:api-server @config)
                       (:api-server (mount/args))))
  :stop (stop api-server))

(def *registered-routes* (atom {}))

(def transit-writer (transit/writer :json))
(def express (nodejs/require "express"))

(defn reg-route! [method path & args]
  (swap! *registered-routes* assoc-in [method path] args))


(def reg-get! (partial reg-route! :get))
(def reg-post! (partial reg-route! :post))
(def reg-put! (partial reg-route! :put))
(def reg-delete! (partial reg-route! :delete))


(defn status
  [response code]
  (.status response code))


(defn send
  ([res data]
   (.format res (clj->js {"application/json" #(.send res (clj->js data))
                          "application/transit+json" #(.send res (transit/write transit-writer data))
                          "default" #(.send res (clj->js data))})))
  ([res status-code data]
    (-> res
      (status status-code)
      (send data))))


(defn query-params [req]
  (aget req "query"))


(defn route-params [req]
  (js->clj (aget req "params") :keywordize-keys true))


(defn stop [api-server]
  (.close (:server @api-server)))


(defn- error-middleware? [f]
  (when (fn? f)
    (= (aget f "length") 4)))


(defn- install-middlewares! [app middlewares]
  (doseq [middleware middlewares]
    (.use app middleware)))


(defn- install-routes! [app routes]
  (doseq [[method routes] routes]
    (doseq [[path args] routes]
      (apply js-invoke app (name method) path args))))


(defn start [{:keys [:port :default-middlewares :middlewares] :as opts
              :or {default-middlewares (keys middleware/default-middlwares)}}]
  (let [app (express)
        middlewares (flatten middlewares)]
    (install-middlewares! app (map middleware/default-middlwares default-middlewares))
    (install-middlewares! app (remove error-middleware? middlewares))
    (install-routes! app @*registered-routes*)
    (install-middlewares! app (filter error-middleware? middlewares))
    {:app app :server (.listen app port)}))


