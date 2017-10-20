(ns dev-server
  (:require [clojure.java.io :as io]
            [compojure.core :as compojure :refer [ANY]]
            [compojure.route :as route]))

(defn index-response [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(def web-handler 
  (-> (compojure/routes
       (ANY "*" []  index-response)
       (route/not-found "Page not found"))))


