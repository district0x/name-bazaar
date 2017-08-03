(ns name-bazaar.server.core
  (:require [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (println "name-bazaar.server.core -main"))

(set! *main-cli-fn* -main)