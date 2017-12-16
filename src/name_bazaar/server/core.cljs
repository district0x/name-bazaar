(ns name-bazaar.server.core
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.config :refer [config]]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.logging]
    [district.server.web3-watcher]
    [medley.core :as medley]
    [mount.core :as mount]
    [name-bazaar.server.api]
    [name-bazaar.server.db]
    [name-bazaar.server.deployer]
    [name-bazaar.server.emailer.core]
    [name-bazaar.server.generator]
    [name-bazaar.server.syncer]
    [name-bazaar.shared.smart-contracts]
    [taoensso.timbre :refer-macros [info warn error]]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:web3 {:port 8545}}}
         :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts}
         :endpoints {:middlewares [logging-middlewares]}
         :web3-watcher {:on-online (fn []
                                     (warn "Ethereum node went online again")
                                     (mount/stop #'name-bazaar.server.db/name-bazaar-db)
                                     (mount/start #'name-bazaar.server.db/name-bazaar-db
                                                  #'name-bazaar.server.syncer/syncer
                                                  #'name-bazaar.server.emailer.core/emailer))
                        :on-offline (fn []
                                      (warn "Ethereum node went offline")
                                      (mount/stop #'name-bazaar.server.syncer/syncer
                                                  #'name-bazaar.server.emailer.core/emailer))}})
    (mount/except [#'name-bazaar.server.deployer/deployer
                   #'name-bazaar.server.generator/generator])
    (mount/start))
  (warn "System started" {:config (medley/dissoc-in @config [:emailer :private-key])}))

(set! *main-cli-fn* -main)
