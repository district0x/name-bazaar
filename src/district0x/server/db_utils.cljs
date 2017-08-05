(ns district0x.server.db-utils
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [cljs.core.async :refer [<! >! chan]]
    [honeysql.core :as sql]
    [medley.core :as medley]
    )
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn log-error [err]
  (when err
    (println err)))

(defn db-run! [db sql-map]
  (let [[query & values] (sql/format sql-map)]
    (.run db query (clj->js values) log-error)))

(defn db-get [db & args]
  (let [[ch [sql-map]] (if (instance? cljs.core.async.impl.channels/ManyToManyChannel (first args))
                         [(first args) (rest args)]
                         [(chan) args])
        [query & values] (sql/format sql-map)]
    (.get db query (clj->js values) (fn [err res]
                                      (log-error err)
                                      (go (>! ch (or res false)))))
    ch))

(defn db-all [db & args]
  (let [[ch [sql-map]] (if (instance? cljs.core.async.impl.channels/ManyToManyChannel (first args))
                         [(first args) (rest args)]
                         [(chan) args])
        [query & values] (sql/format sql-map)]
    (.all db query (clj->js values) (fn [err res]
                                      (log-error err)
                                      (go (>! ch (js->clj (or res []) :keywordize-keys true)))))
    ch))

(defn keyword->sql-col [kw]
  (keyword (name kw)))

(defn keywords->sql-cols [kws]
  (map keyword->sql-col kws))

(def sql-results-map->seq (comp (map (partial map vals))
                                (map flatten)))

(defn sql-results-chan [select-fields]
  (if (< 1 (count select-fields))
    (chan 1 (map (partial transform-keys cs/->kebab-case-keyword)))
    (chan 1 sql-results-map->seq)))

