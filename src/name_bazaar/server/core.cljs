(ns name-bazaar.server.core
  (:require
    [cljs.core.async :refer [<! >! chan take!]]
    [cljs.nodejs :as nodejs]
    [district0x.server.api-server :as api-server]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state :refer [*server-state*]]
    [name-bazaar.server.api]
    [name-bazaar.server.db-sync :as db-sync]
    [name-bazaar.server.emailer.listeners :as listeners]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(nodejs/enable-util-print!)
(set! js/XMLHttpRequest (nodejs/require "xhr2"))

(defn -main [& _]
  (d0x-effects/load-config! *server-state* state/default-config)
  (go
    (d0x-effects/create-web3! *server-state* {:port (state/config :mainnet-port)})
    (d0x-effects/create-db! *server-state*)
    (d0x-effects/load-smart-contracts! *server-state* smart-contracts)
    (api-server/start! (state/config :api-port))
    (<! (d0x-effects/load-my-addresses! *server-state*))
    (db-sync/start-syncing! @*server-state*)
    (listeners/setup-event-listeners! *server-state*)))

(set! *main-cli-fn* -main)
