(ns name-bazaar.server.core
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.api-server.middleware.logging :refer [logging-middlewares]]
    [district.server.logging.core]
    [district.server.web3-watcher.core]
    [mount.core :as mount]
    [name-bazaar.server.api]
    [name-bazaar.server.emailer.core]
    [name-bazaar.server.syncer]
    [name-bazaar.shared.smart-contracts]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts}})
    (mount/start)))

(set! *main-cli-fn* -main)
