(ns name-bazaar.ui.config)

;; TODO currently this is configuration in source code
;; you need to rebuild UI for each new configuration
;; (e.g. change this file then build new docker UI image)
;; we should make UI configurable on the fly
(goog-define environment "prod")

(def development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :pushroute-hosts "localhost"
   :infura-id "0ff2cb560e864d078290597a29e2505d"
   :load-node-addresses? true
   :root-url "http://0.0.0.0:4544"
   :server-url "http://localhost:6200"})

(def production-config
  {:logging {:level :warn
             :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"}}
   :pushroute-hosts "namebazaar.io"
   :infura-id "0ff2cb560e864d078290597a29e2505d"
   :load-node-addresses? false
   :root-url "https://namebazaar.io"
   :server-url "https://api.namebazaar.io"})

(def config
  (condp = environment
    "prod" production-config
    "dev" development-config))
