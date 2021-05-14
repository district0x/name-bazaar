(ns name-bazaar.server.core
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.config :refer [config]]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.logging]
    [district.server.web3-events]
    [district.shared.async-helpers :as async-helpers]
    [medley.core :as medley]
    [mount.core :as mount]
    [name-bazaar.server.api]
    [name-bazaar.server.db]
    [name-bazaar.server.emailer]
    [name-bazaar.server.syncer]
    [name-bazaar.shared.smart-contracts]
    [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (async-helpers/extend-promises-as-channels!)
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true
                                      :sentry {:dsn "https://597ef71a10a240b0949c3b482fe4b9a4@sentry.io/1364232"
                                               :min-level :warn}}
                            :web3 {:url "ws://127.0.0.1:8549"
                                   :on-online (fn []
                                                (log/warn "Ethereum node went online again")
                                                (mount/start #'name-bazaar.server.db/name-bazaar-db
                                                             #'district.server.web3-events/web3-events
                                                             #'name-bazaar.server.syncer/syncer
                                                             #'name-bazaar.server.emailer/emailer))
                                   :on-offline (fn []
                                                 (log/warn "Ethereum node went offline")
                                                 (mount/stop #'name-bazaar.server.db/name-bazaar-db
                                                             #'district.server.web3-events/web3-events
                                                             #'name-bazaar.server.syncer/syncer
                                                             #'name-bazaar.server.emailer/emailer))}
                            :web3-events {:events {:ens/new-owner [:ens :NewOwner]
                                                   :ens/transfer [:ens :Transfer]
                                                   :offering-registry/offering-added [:offering-registry :onOfferingAdded]
                                                   :offering-registry/offering-changed [:offering-registry :onOfferingChanged]
                                                   :offering-requests/request-added [:offering-requests :onRequestAdded]
                                                   :offering-requests/round-changed [:offering-requests :onRoundChanged]
                                                   :registrar/transfer [:eth-registrar :Transfer]}}
                            :ui {:reveal-period {:hours 48}
                                 :etherscan-url "https://etherscan.io"
                                 :cryptocompare-api-key "INSERT-YOUR-API-KEY-HERE"}}}
         :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts}
         :endpoints {:middlewares [logging-middlewares]}})
      (mount/start))
  (log/warn "System started" {:config (medley/dissoc-in @config [:emailer :private-key])}))

(set! *main-cli-fn* -main)
