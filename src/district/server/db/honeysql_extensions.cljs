(ns district.server.db.honeysql-extensions
  (:require
    [honeysql.format :as sql-format :refer [to-sql paren-wrap]]))

(swap! sql-format/clause-store assoc :insert-or-replace-into (:insert-into @sql-format/clause-store))

(defn get-first
  "Returns the first element if the passed argument is a collection, else return the passed argument
   as is."
  [x]
  (if (sequential? x)
    (first x)
    x))


(defmethod sql-format/format-clause :insert-or-replace-into [[_ table] _]
  (if (and (sequential? table) (sequential? (first table)))
    (str "INSERT OR REPLACE INTO "
         (sql-format/to-sql (ffirst table))
         " (" (sql-format/comma-join (map sql-format/to-sql (second (first table)))) ") "
         (sql-format/to-sql (second table)))
    (str "INSERT OR REPLACE INTO " (to-sql table))))


(extend-protocol sql-format/ToSql
  boolean
  (to-sql [x]
    (if x 1 0)))


(defmethod sql-format/format-clause :create-index [[_ tablename] _]
  (str "CREATE INDEX " (-> tablename
                         get-first
                         to-sql)))


(defmethod sql-format/format-clause :on [[_ [tablename column]] _]
  (str "ON " (get-first (to-sql tablename)) " " (paren-wrap (to-sql column))))

