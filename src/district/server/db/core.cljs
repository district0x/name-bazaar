(ns district.server.db.core
  (:refer-clojure :exclude [get run!])
  (:require
    [clojure.string :as string]
    [district.server.config.core :refer [config]]
    [district.server.db.honeysql-extensions]
    [honeysql.core :as sql]
    [honeysql.format :as sql-format]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} db
  :start (start (merge (:db @config)
                       (:db (mount/args))))
  :stop (stop db))

(def Sqlite3Database (js/require "better-sqlite3"))
(def *transform-result-keys-fn* (atom nil))

(defn start [{:keys [:path :opts :transform-result-keys-fn :sql-name-transform-fn]
              :or {path "db.db"
                   opts {:memory true}
                   sql-name-transform-fn (comp #(string/replace % "_STAR_" "*")
                                               #(string/replace % "_PERCENT_" "%")
                                               munge)
                   transform-result-keys-fn (comp keyword demunge)}}]

  (set! *transform-result-keys-fn* transform-result-keys-fn)
  (set! sql-format/*name-transform-fn* sql-name-transform-fn)

  (new Sqlite3Database path (clj->js opts)))


(defn stop [db]
  (.close @db))


(defn- map-keys [f m]
  (into {} (map (fn [[k v]] [(f k) v]) m)))


(defn run! [sql-map]
  (let [[query & values] (sql/format sql-map)]
    (js->clj (.run (.prepare @db query) (clj->js (or values []))) :keywordize-keys true)))


(defn get [sql-map]
  (let [[query & values] (sql/format sql-map)]
    (map-keys *transform-result-keys-fn* (js->clj (.get (.prepare @db query) (clj->js (or values [])))))))


(defn all [sql-map]
  (let [[query & values] (sql/format sql-map)]
    (map (partial map-keys *transform-result-keys-fn*)
         (js->clj (.all (.prepare @db query) (clj->js (or values [])))))))


(defn ->count-query [sql-map & [{:keys [:count-distinct-column :count-select]
                                 :or {count-select [:%count.*]}}]]
  (let [select (if (contains? (set (:modifiers sql-map)) :distinct)
                 [(sql/call :count-distinct (or count-distinct-column (first (:select sql-map))))]
                 count-select)]
    (-> sql-map
      (assoc :select select)
      (dissoc :offset :limit :order-by))))


(defn total-count [sql-map & [opts]]
  (second (first (get (->count-query sql-map opts)))))


(defn order-by-closest-like [col-name s & [{:keys [:suffix :prefix]}]]
  (sql/call :case
            [:= col-name (str prefix s suffix)] 1
            [:like col-name (str prefix s "%" suffix)] 2
            [:like col-name (str prefix "%" s "%" suffix)] 3
            :else 4))