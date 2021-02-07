(ns cljs.user
  (:require
   [name-bazaar.server.dev]
   [taoensso.timbre :as log]))

(log/info "Initialized Development Environment!")
(log/info "Run (redeploy) in the CLJS REPL for a fresh environment.")
(log/info "Run (generate-data) to initialize sample dev data")

(defn redeploy
  "Redeploy Smart Contracts"
  []
  (name-bazaar.server.dev/redeploy)
  (log/info "Redeployment Finished!"))

(defn generate-data
  "Generate sample dev data"
  []
  (name-bazaar.server.dev/generate-data)
  (log/info "Data generation Finished!"))

(set! *main-cli-fn* name-bazaar.server.dev/-main)
