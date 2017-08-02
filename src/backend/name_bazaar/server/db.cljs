(ns name-bazaar.server.db
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.state :as state]
    [honeysql.core :as sql]
    [honeysql.format :as sql-format]
    [honeysql.helpers :as sql-helpers :refer [merge-where]]
    [cljs.spec.alpha :as s])
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

(defn create-tables! [db]
  (.serialize db (fn []
                   (.run db "CREATE TABLE offerings (
                          address CHAR(42) PRIMARY KEY NOT NULL,
                          created_on UNSIGNED INTEGER NOT NULL,
                          name VARCHAR NOT NULL,
                          original_owner CHAR(42) NOT NULL,
                          new_owner CHAR(42) DEFAULT NULL,
                          version UNSIGNED INTEGER NOT NULL,
                          price UNSIGNED BIG INT NOT NULL,
                          end_time UNSIGNED INTEGER DEFAULT NULL,
                          is_node_owner BOOLEAN NOT NULL DEFAULT false
                          )" log-error)
                   (.run db "CREATE INDEX created_on_index ON offerings (created_on)" log-error)
                   (.run db "CREATE INDEX name_index ON offerings (name)" log-error)
                   (.run db "CREATE INDEX original_owner_index ON offerings (original_owner)" log-error)
                   (.run db "CREATE INDEX new_owner_index ON offerings (new_owner)" log-error)
                   (.run db "CREATE INDEX price_index ON offerings (price)" log-error)
                   (.run db "CREATE INDEX end_time_index ON offerings (end_time)" log-error)
                   (.run db "CREATE INDEX is_node_owner_index ON offerings (is_node_owner)" log-error)
                   (.run db "CREATE INDEX version_index ON offerings (version)" log-error)

                   (.run db "CREATE TABLE offering_requests (
                          node CHAR(66) PRIMARY KEY NOT NULL,
                          name VARCHAR NOT NULL,
                          requests_count UNSIGNED INTEGER NOT NULL DEFAULT 0
                          )" log-error)

                   (.run db "CREATE INDEX requests_count_index ON offering_requests (requests_count)" log-error))))

(def offering-keys [:offering/address
                    :offering/created-on
                    :offering/name
                    :offering/original-owner
                    :offering/new-owner
                    :offering/version
                    :offering/price
                    :english-auction-offering/end-time
                    :offering/node-owner?])

(def offering-columns (-> offering-keys
                        butlast
                        (concat ["is_node_owner"])
                        (->> (map (comp keyword name)))))

(defn upsert-offering! [db values]
  (db-run! db {:insert-or-replace-into :offerings
               :columns offering-columns
               :values [((apply juxt offering-keys) values)]}))

(defn set-offering-node-owner?! [db {:keys [:offering/node-owner? :offering/address]}]
  (db-run! db {:update :offerings
               :set {:is-node-owner node-owner?}
               :where [:= :address address]}))

(defn offering-exists? [db offering-address]
  (db-get db {:select [1]
              :from [:offerings]
              :where [:= :address offering-address]}))

(def result-map->seq (comp (map (partial map vals))
                           (map flatten)))

(def offering-requests-keys [:offering-request/node :offering-request/name :offering-request/requesters-count])

(defn upsert-offering-requests! [db values]
  (db-run! db {:insert-or-replace-into :offering-requests
               :columns (map (comp keyword name) offering-requests-keys)
               :values [((apply juxt offering-requests-keys) values)]}))


(s/def ::order-by-dir (partial contains? #{:desc :asc}))
(s/def ::offering-order-by-column (partial contains? #{:price :end-time :created-on}))
(s/def ::offerings-order-by-item (s/tuple ::offering-order-by-column ::order-by-dir))
(s/def ::offerings-order-by (s/coll-of ::offerings-order-by-item :kind vector? :distinct true))

(defn search-offerings [db {:keys [:original-owner :new-owner :name :min-price :max-price
                                   :max-end-time version :node-owner? :limit :offset :order-by]
                            :or {offset 0 limit -1}}]
  (db-all db
          (chan 1 result-map->seq)
          (cond-> {:select [:address]
                   :from [:offerings]
                   :offset offset
                   :limit limit}
            original-owner (merge-where [:= :original-owner original-owner])
            new-owner (merge-where [:= :new-owner new-owner])
            min-price (merge-where [:>= :price min-price])
            max-price (merge-where [:<= :price max-price])
            max-end-time (merge-where [:<= :end-time max-end-time])
            version (merge-where [(if (= (keyword version) :instant-buy-offering) :< :>=)
                                        :version 100000])
            (boolean? node-owner?) (merge-where [:= :is-node-owner node-owner?])
            name (merge-where [:like :name name])
            (s/valid? ::offerings-order-by order-by) (merge {:order-by order-by}))))

(s/def ::offering-requests-order-by-column (partial contains? #{:requests-count}))
(s/def ::offering-requests-order-by-item (s/tuple ::offering-requests-order-by-column ::order-by-dir))
(s/def ::offering-requests-order-by (s/coll-of ::offering-requests-order-by-item :kind vector? :distinct true))

(defn search-offering-requests [db {:keys [:limit :offset :name :order-by]
                                    :or {offset 0 limit -1}}]
  (db-all db
          (chan 1 result-map->seq)
          (cond-> {:select [:node]
                   :from [:offering-requests]
                   :offset offset
                   :limit limit}
            name (merge-where [:like :name name])
            (s/valid? ::offering-requests-order-by order-by) (merge {:order-by order-by}))))



(swap! sql-format/clause-store assoc :insert-or-replace-into (:insert-into @sql-format/clause-store))

(defmethod sql-format/format-clause :insert-or-replace-into [[_ table] _]
  (if (and (sequential? table) (sequential? (first table)))
    (str "INSERT OR REPLACE INTO "
         (sql-format/to-sql (ffirst table))
         " (" (sql-format/comma-join (map sql-format/to-sql (second (first table)))) ") "
         (sql-format/to-sql (second table)))
    (str "INSERT OR REPLACE INTO " (sql-format/to-sql table))))

(extend-protocol sql-format/ToSql
  boolean
  (to-sql [x]
    (if x 1 0)))

