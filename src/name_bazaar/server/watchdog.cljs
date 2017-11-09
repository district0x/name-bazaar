(ns name-bazaar.server.watchdog
  (:require
    [cemerick.url :as url]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan timeout]]
    [clojure.string :as string]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state :refer [*server-state*]]
    [name-bazaar.server.db-sync :as db-sync]
    [name-bazaar.server.emailer.listeners :as email-listeners]
    [taoensso.timbre :refer-macros [log trace debug info warn error]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn stop-node-watchdog! []
  (swap! *server-state* update :node-watchdog merge {:enabled? false :online? false}))

(defn start-node-watchdog! [on-up-fn on-down-fn]
  (swap! *server-state* update :node-watchdog merge {:enabled? true :online? false})
  (info "Starting watcher")
  (go-loop []
           (let [node-watchdog (:node-watchdog @*server-state*)]
             (<! (timeout (:timeout node-watchdog)))
             (let [web3 (state/web3)
                   host (aget web3 "currentProvider" "host")
                   {:keys [:port]} (url/url host)
                   state (or
                           ;; For testrpc connected? doesn't work
                           (= port (state/config :testrpc-port))
                           (web3/connected? web3))]
               (when (and
                       on-down-fn
                       (:online? node-watchdog)
                       (not state))
                 (warn "Node is offline" {:host host})
                 (on-down-fn))
               (when (and
                       on-up-fn
                       (not (:online? node-watchdog))
                       state)
                 (warn "Node is online" {:host host})
                 (on-up-fn))
               (when (:enabled? node-watchdog)
                 (swap! *server-state* assoc-in [:node-watchdog :online?] state)
                 (recur))))))


(defn start-syncing! []
  (start-node-watchdog! (fn []
                          (go
                            (<! (d0x-effects/create-db!))
                            (db-sync/start-syncing!)
                            (email-listeners/setup-event-listeners!)))
                        (fn []
                          (db-sync/stop-syncing!)
                          (email-listeners/stop-event-listeners!))))

(defn stop-syncing! []
  (stop-node-watchdog!)
  (db-sync/stop-syncing!)
  (email-listeners/stop-event-listeners!))
