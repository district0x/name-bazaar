(ns user
  (:require
   [com.rpl.specter :as s] 
   [figwheel-sidecar.repl-api :as fw-repl]
   [figwheel-sidecar.config :as fw-config]
   [taoensso.timbre :as timbre :refer [info]]))


(defn set-closure-define
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
  [& {:keys [ui-only?] :or {ui-only? false}}]
  (let [environment (if ui-only? "prod" "dev")
        fig-config  (fw-config/fetch-config)]
    (when ui-only? (info "Performing ui-only build..."))
    (fw-repl/start-figwheel!
     (set-closure-define fig-config "dev-ui" 'name-bazaar.ui.db.environment environment)
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
