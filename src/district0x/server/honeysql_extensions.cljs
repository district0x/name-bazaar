(ns district0x.server.honeysql-extensions
  (:require [honeysql.format :as sql-format]))

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

