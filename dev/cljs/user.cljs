(ns cljs.user
  (:require
   [name-bazaar.server.dev]
   [taoensso.timbre :as log]))

(log/info "Initialized Development Environment!")
(log/info "Run (redeploy) in the CLJS REPL for a fresh environment.")

(defn redeploy
  "Redeploy Smart Contracts"
  []
  (log/info "Redeployment is a long process, please be patient...")
  (.nextTick
   js/process
   (fn []
     (name-bazaar.server.dev/redeploy)
     (log/info "Redeployment Finished!"))))

(set! *main-cli-fn* name-bazaar.server.dev/-main)
