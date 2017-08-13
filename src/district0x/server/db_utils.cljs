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

(defn- db-run! [db sql-map & [{:keys [:get-last-id?]}]]
  (let [[query & values] (sql/format sql-map)
        ch (chan)]
    (.run db query (clj->js values) (fn [err res]
                                      (if err
                                        (log-error err)
                                        (if get-last-id?
                                          (this-as this
                                            (go (>! ch (aget this "lastID"))))
                                          (go (>! ch true))))))
    ch))

(defn db-get [db & args]
  (let [[ch [sql-map]] (if (instance? cljs.core.async.impl.channels/ManyToManyChannel (first args))
                         [(first args) (rest args)]
                         [(chan) args])
        [query & values] (sql/format sql-map)]
    (.get db query (clj->js values) (fn [err res]
                                      (log-error err)
                                      (go (>! ch (if res
                                                   (-> res
                                                     (js->clj :keywordize-keys true)
                                                     (->> (transform-keys cs/->kebab-case-keyword)))
                                                   false)))))
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

(defn order-by-closest-like [col-name s {:keys [:suffix :prefix]}]
  (sql/call :case
            [:= col-name (str prefix s suffix)] 1
            [:like col-name (str prefix s "%" suffix)] 2
            [:like col-name (str prefix "%" s "%" suffix)] 3
            :else 4))
