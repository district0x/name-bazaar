(ns dev-server
  (:require [clojure.java.io :as io]
            [compojure.core :as compojure :refer [ANY]]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.default-charset :refer [wrap-default-charset]]))

;; CREDIT : https://github.com/mhuebert/figwheel-pushstate-server

(defn- wrap [handler middleware options]
  (if (true? options)
    (middleware handler)
    (if options
      (middleware handler options)
      handler)))

(defn wrap-defaults
  "Wraps a handler in default Ring middleware, as specified by the supplied
  configuration map.
  See: api-defaults
       site-defaults
       secure-api-defaults
       secure-site-defaults"
  [handler config]
  (-> handler
      (wrap wrap-resource         (get-in config [:static :resources] false))
      (wrap wrap-file             (get-in config [:static :files] false))
      (wrap wrap-content-type     (get-in config [:responses :content-types] false))
      (wrap wrap-default-charset  (get-in config [:responses :default-charset] false))))

(def web-handler 
  (-> (compojure/routes
       (ANY "*" _  {:status 200
                    :headers {"Content-Type" "text/html; charset=utf-8"}
                    :body (io/input-stream (io/resource "public/index.html"))}))
      (wrap-defaults defaults/site-defaults)))


