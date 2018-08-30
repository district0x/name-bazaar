(ns user
  (:require [figwheel-sidecar.repl-api]))


(defn start-server! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
              [:data :figwheel-options :server-port] 4541)
    "dev-server")
  (figwheel-sidecar.repl-api/cljs-repl "dev-server"))


(defn start-ui!
  [& {:keys [ui-only?] :or {ui-only? false}}]
  (let [dev-build-id (if ui-only? "dev-ui-only" "dev-ui")]
   (figwheel-sidecar.repl-api/start-figwheel!
     (figwheel-sidecar.config/fetch-config)
     dev-build-id)
   (figwheel-sidecar.repl-api/cljs-repl dev-build-id)))


(defn start-tests! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
              [:data :figwheel-options :server-port] 4543)
    "server-tests")
  (figwheel-sidecar.repl-api/cljs-repl "server-tests"))


(comment
  (start-ui!)
  (start-server!)
  (start-tests!))
