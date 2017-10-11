(ns name-bazaar.server.watchdog
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan timeout]]
    [clojure.string :as string]
    [district0x.server.state :as state]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state :refer [*server-state*]]
    [name-bazaar.server.db-sync :as db-sync]
    [taoensso.timbre :refer-macros [log trace debug info warn error]]
    [name-bazaar.server.emailer.listeners :as email-listeners])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn stop-node-watchdog! [server-state]
  (swap! server-state assoc-in [:node-watchdog :enabled?] false))

(defn start-node-watchdog! [server-state on-up-fn on-down-fn]
  (swap! server-state assoc-in [:node-watchdog :enabled?] true)
  (let [shortcircuit? (get-in @server-state [:config :shortcircuit-node-watchdog?])]
    (if shortcircuit?
      (warn "Shortcircuit watcher")
      (info "Starting watcher"))
    (go-loop []
      (let [node-watchdog (:node-watchdog @server-state)]
        (<! (timeout (:timeout node-watchdog)))
        (info "Check?")
        (let [state (or shortcircuit?
                        (web3/connected? (state/web3 @server-state)))]
          (when (and
                 on-down-fn
                 (:online? node-watchdog)
                 (not state))
            (warn "Node is offline")
            (on-down-fn))
          (when (and
                 on-up-fn
                 (not (:online? node-watchdog))
                 state)
            (warn "Node is online")
            (on-up-fn))
          (swap! server-state assoc-in [:node-watchdog :online?] state))
        (when (:enabled? node-watchdog)
          (recur))))))


(defn start-syncing! [server-state]
  (start-node-watchdog! server-state
                        (fn []
                          (d0x-effects/create-db! server-state)
                          (db-sync/start-syncing! @server-state)
                          (email-listeners/setup-event-listeners! server-state))
                        (fn []
                          (db-sync/stop-syncing!)
                          (email-listeners/stop-event-listeners!))))

(defn stop-syncing! [server-state]
  (stop-node-watchdog! server-state)
  (db-sync/stop-syncing!))
