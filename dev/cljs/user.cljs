(ns cljs.user
  (:require
   [name-bazaar.server.dev]
   [taoensso.timbre :as timbre :refer-macros [warn info]]))


(info "Initialized Development Environment!")
(info "Run (redeploy) in the CLJS REPL for a fresh environment.")



(defn redeploy 
  "Redeploy Smart Contracts"
  []
  (warn "Redeployment is a long process, please be patient...")
  (.nextTick
   js/process
   (fn []
     (name-bazaar.server.dev/redeploy)
     (info "Redeployment Finished!"))))


(set! *main-cli-fn* name-bazaar.server.dev/-main)
