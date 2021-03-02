(ns cljs.user
  (:require
   [name-bazaar.server.dev]
   [taoensso.timbre :as log]))

(log/info "Initialized Development Environment!")
(log/info "Run (redeploy) in the CLJS REPL for a fresh environment.")
(log/info "Run (generate-data) to initialize sample dev data")
(log/info "Run (deploy-contracts) to deploy contracts anywhere based on config.edn")

(defn redeploy
  "Redeploy smart contracts to dev blockchain"
  []
  (name-bazaar.server.dev/redeploy)
  (log/info "Redeployment finished!"))

(defn generate-data
  "Generate sample dev data"
  []
  (name-bazaar.server.dev/generate-data)
  (log/info "Data generation finished!"))

(defn deploy-contracts
  "Deploy smart contracts anywhere"
  []
  (name-bazaar.server.dev/deploy-contracts)
  (log/info "Deployment finished!"))

(set! *main-cli-fn* name-bazaar.server.dev/-main)
