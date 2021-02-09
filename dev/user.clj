(ns user
  (:require
   [com.rpl.specter :as s]
   [figwheel-sidecar.repl-api :as fw-repl]
   [figwheel-sidecar.config :as fw-config]
   [taoensso.timbre :as log]
   [compojure.core :refer [defroutes]]
   [compojure.route :as route]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn- wrap-default-index [next-handler]
  (fn [request]
    (next-handler (assoc request :uri "/index.html"))))

(defroutes routes (route/resources "/"))

;; Inspired by https://github.com/bhauman/lein-figwheel/issues/344#issuecomment-243918040
(def route-handler
  "This function is responsible to serve index.html on every web route.
  See :ring-handler in project.clj."
  (wrap-defaults (wrap-default-index routes) site-defaults))

(defn- set-closure-define
  "Sets the :closure-defines for the given `build-id` in the given
  figwheel config `config`"
  [config build-id key value]
  (s/setval
   [:data :all-builds (s/filterer #(= (:id %) build-id)) s/FIRST
    (s/keypath :build-options :closure-defines key)]
   value config))


(defn start-server! []
  (fw-repl/start-figwheel!
   (assoc-in (fw-config/fetch-config)
             [:data :figwheel-options :server-port] 4541)
   "dev-server")
  (fw-repl/cljs-repl "dev-server"))


(defn start-ui!
  "Start the client build.
  Passing {:prod-config? true} points the client to the production server and logging service!"
  [& {:keys [:prod-config?]}]
  (let [environment (if prod-config? "prod" "dev")
        fig-config  (fw-config/fetch-config)]
    (when prod-config? (log/info "Performing ui-only build..."))
    (fw-repl/start-figwheel!
     (set-closure-define fig-config "dev-ui" 'name-bazaar.ui.config.environment environment)
     "dev-ui")
    (fw-repl/cljs-repl "dev-ui")))


(defn start-tests! []
  (fw-repl/start-figwheel!
   (assoc-in (fw-config/fetch-config)
             [:data :figwheel-options :server-port] 4543)
   "server-tests")
  (fw-repl/cljs-repl "server-tests"))


(comment
  (start-ui!)
  (start-server!)
  (start-tests!))
