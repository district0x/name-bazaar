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
    [name-bazaar.server.emailer]
    [name-bazaar.server.syncer]
    [name-bazaar.shared.smart-contracts]
    [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true
                                      :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"
                                               :min-level :warn}}
                            :web3 {:port 8545}
                            :ui {:reveal-period {:hours 48}
                                 :etherscan-url "https://etherscan.io"}}}
         :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts}
         :endpoints {:middlewares [logging-middlewares]}
         :web3-watcher {:on-online (fn []
                                     (log/warn "Ethereum node went online again")
                                     (mount/stop #'name-bazaar.server.db/name-bazaar-db)
                                     (mount/start #'name-bazaar.server.db/name-bazaar-db
                                                  #'name-bazaar.server.syncer/syncer
                                                  #'name-bazaar.server.emailer/emailer))
                        :on-offline (fn []
                                      (log/warn "Ethereum node went offline")
                                      (mount/stop #'name-bazaar.server.syncer/syncer
                                                  #'name-bazaar.server.emailer/emailer))}})
      (mount/except [#'name-bazaar.server.deployer/deployer])
      (mount/start))
  (log/warn "System started" {:config (medley/dissoc-in @config [:emailer :private-key])}))

(set! *main-cli-fn* -main)
