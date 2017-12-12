(ns district.server.web3-watcher.core
  (:require
    [cljs-web3.core :as web3]
    [district.server.config.core :refer [config]]
    [district.server.web3.core :refer [web3]]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)
(defstate web3-watcher
  :start (start (merge (:web3-watcher @config)
                       (:web3-watcher (mount/args))))
  :stop (stop))


(defn online? []
  @(:online? @web3-watcher))


(defn- update-online! [online?]
  (reset! (:online? @web3-watcher) online?))


(defn check-connection []
  (let [connected? (web3/connected? @web3)]
    (cond
      (and connected? (not (online?)))
      (do
        (update-online! true)
        ((:on-online @web3-watcher)))

      (and (not connected?) (online?))
      (do
        (update-online! false)
        ((:on-offline @web3-watcher))))))


(defn start [{:keys [:interval :on-online :on-offline] :as opts}]
  (merge {:on-online identity
          :on-offline identity
          :interval 3000
          :interval-id (js/setInterval check-connection interval)
          :online? (atom (web3/connected? @web3))}
         opts))


(defn stop []
  (js/clearInterval (:interval-id @web3-watcher)))
