(ns name-bazaar.server.db
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [cljs.spec.alpha :as s]
    [district0x.server.honeysql-extensions]
    [district0x.server.state :as state]
    [district0x.shared.utils :as d0x-shared-utils :refer [combination-of? collify]]
    [district0x.server.db-utils :refer [log-error db-get db-run! db-all keywords->sql-cols sql-results-chan]]
    [honeysql.helpers :as sql-helpers :refer [merge-where]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn create-tables! [db]
  (.serialize db (fn []
                   (.run db "CREATE TABLE offerings (
                          address CHAR(42) PRIMARY KEY NOT NULL,
                          created_on UNSIGNED INTEGER NOT NULL,
                          node CHAR(66) NOT NULL,
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
                   (.run db "CREATE INDEX node_index ON offerings (node)" log-error)
                   (.run db "CREATE INDEX original_owner_index ON offerings (original_owner)" log-error)
                   (.run db "CREATE INDEX new_owner_index ON offerings (new_owner)" log-error)
                   (.run db "CREATE INDEX price_index ON offerings (price)" log-error)
                   (.run db "CREATE INDEX end_time_index ON offerings (end_time)" log-error)
                   (.run db "CREATE INDEX is_node_owner_index ON offerings (is_node_owner)" log-error)
                   (.run db "CREATE INDEX version_index ON offerings (version)" log-error)

                   (.run db "CREATE TABLE ens_records (
                          node CHAR(66) NOT NULL,
                          last_offering CHAR(42) NOT NULL,
                          FOREIGN KEY(last_offering) REFERENCES offerings(address)
                          )" log-error)

                   (.run db "CREATE TABLE offering_requests (
                          node CHAR(66) PRIMARY KEY NOT NULL,
                          name VARCHAR NOT NULL,
                          requesters_count UNSIGNED INTEGER NOT NULL DEFAULT 0
                          )" log-error)

                   (.run db "CREATE INDEX requesters_count_index ON offering_requests (requesters_count)" log-error))))

(def offering-keys [:offering/address
                    :offering/created-on
                    :offering/node
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
                        keywords->sql-cols))

(defn upsert-offering! [db values]
  (db-run! db {:insert-or-replace-into :offerings
               :columns offering-columns
               :values [((apply juxt offering-keys) values)]}))

(def ens-record-columns [:ens.record/node
                         :ens.record/last-offering])

(defn upsert-ens-record! [db values]
  (db-run! db {:insert-or-replace-into :ens-records
               :columns (keywords->sql-cols ens-record-columns)
               :values [((apply juxt ens-record-columns) values)]}))

(defn set-offering-node-owner?! [db {:keys [:offering/node-owner? :offering/address]}]
  (db-run! db {:update :offerings
               :set {:is-node-owner node-owner?}
               :where [:= :address address]}))

(defn offering-exists? [db offering-address]
  (db-get db {:select [1]
              :from [:offerings]
              :where [:= :address offering-address]}))

(def offering-requests-keys [:offering-request/node :offering-request/name :offering-request/requesters-count])

(defn upsert-offering-requests! [db values]
  (db-run! db {:insert-or-replace-into :offering-requests
               :columns (keywords->sql-cols offering-requests-keys)
               :values [((apply juxt offering-requests-keys) values)]}))


(s/def ::order-by-dir (partial contains? #{:desc :asc}))
(s/def ::offering-order-by-column (partial contains? #{:price :end-time :created-on}))
(s/def ::offerings-order-by-item (s/tuple ::offering-order-by-column ::order-by-dir))
(s/def ::offerings-order-by (s/coll-of ::offerings-order-by-item :kind vector? :distinct true))
(s/def ::offerings-select-fields (partial combination-of? #{:address :node :version}))

(defn search-offerings [db {:keys [:original-owner :new-owner :node :name :min-price :max-price
                                   :max-end-time :version :node-owner? :limit :offset :order-by
                                   :select-fields]
                            :or {offset 0 limit -1}}]
  (let [select-fields (collify select-fields)
        select-fields (if (s/valid? ::offerings-select-fields select-fields) select-fields [:address])]
    (db-all db
            (sql-results-chan select-fields)
            (cond-> {:select select-fields
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
              node (merge-where [:= :node node])
              name (merge-where [:like :name name])
              (s/valid? ::offerings-order-by order-by) (merge {:order-by order-by})))))

(s/def ::ens-records-select-fields (partial combination-of? #{:node :last-offering}))

(defn search-ens-records [db {:keys [:nodes :select-fields :offset :limit]
                              :or {offset 0 limit -1}}]
  (let [select-fields (collify select-fields)
        select-fields (if (s/valid? ::ens-records-select-fields select-fields) select-fields [:node])]
    (db-all db
            (sql-results-chan select-fields)
            (cond-> {:select select-fields
                     :from [:ens-records]
                     :offset offset
                     :limit limit}
              nodes (merge-where [:in :node (collify nodes)])))))

(s/def ::offering-requests-order-by-column (partial contains? #{:requests-count}))
(s/def ::offering-requests-order-by-item (s/tuple ::offering-requests-order-by-column ::order-by-dir))
(s/def ::offering-requests-order-by (s/coll-of ::offering-requests-order-by-item :kind vector? :distinct true))

(defn search-offering-requests [db {:keys [:limit :offset :name :order-by]
                                    :or {offset 0 limit -1}}]
  (db-all db
          (cond-> {:select [:node]
                   :from [:offering-requests]
                   :offset offset
                   :limit limit}
            name (merge-where [:like :name name])
            (s/valid? ::offering-requests-order-by order-by) (merge {:order-by order-by}))))


