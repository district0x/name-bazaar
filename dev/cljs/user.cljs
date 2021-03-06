(ns cljs.user
  (:require
   [name-bazaar.server.dev]
   [taoensso.timbre :as log]))

(log/info "Initialized Development Environment!")
(log/info "Run (generate-data) to initialize sample dev data")


(defn generate-data
  "Generate sample dev data"
  []
  (name-bazaar.server.dev/generate-data)
  (log/info "Data generation finished!"))

(set! *main-cli-fn* name-bazaar.server.dev/-main)
