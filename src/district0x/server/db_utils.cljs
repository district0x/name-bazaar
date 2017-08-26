(ns district0x.server.db-utils
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [cljs.core.async :refer [<! >! chan put!]]
    [cljs.spec.alpha :as s]
    [district0x.server.utils :as d0x-server-utils]
    [honeysql.core :as sql]
    [medley.core :as medley]
    [clojure.string :as string])
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
                                            (put! ch (aget this "lastID")))
                                          (put! ch true)))))
    ch))

(defn ->count-query [sql-map]
  (-> sql-map
    (assoc :select [:%count.*])
    (dissoc :offset :limit :order-by)))

(defn db-get [db sql-map & [{:keys [:port]
                             :or {port (chan)}}]]
  (let [[query & values] (sql/format sql-map)]
    (.get db query (clj->js values) (fn [err res]
                                      (log-error err)
                                      (put! port (if res
                                                   (-> res
                                                     (js->clj :keywordize-keys true)
                                                     (->> (transform-keys cs/->kebab-case-keyword)))
                                                    false))))
    port))

(defn db-all [db sql-map & [{:keys [:port :total-count?]
                             :or {port (chan)}}]]
  (let [result-ch (chan)
        total-count-ch (chan)
        [query & values] (sql/format sql-map)]
    (if total-count?
      (let [[query & values] (sql/format (->count-query sql-map))]
        (.get db query (clj->js values) (fn [err res]
                                          (log-error err)
                                          (put! total-count-ch (aget res "count(*)")))))
      (put! total-count-ch false))
    (.all db query (clj->js values) (fn [err res]
                                      (log-error err)
                                      (put! port (->> (js->clj (or res []) :keywordize-keys true)
                                                   (map (partial transform-keys cs/->kebab-case-keyword))))))
    (go
      (let [total-count (<! total-count-ch)
            items (<! port)]
        (put! result-ch (merge {:items items}
                               (when total-count
                                 {:total-count total-count})))))
    result-ch))

(defn keyword->sql-col [kw]
  (keyword (string/replace (name kw) "?" "")))

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
