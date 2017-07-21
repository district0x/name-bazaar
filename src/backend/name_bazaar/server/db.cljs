(ns name-bazaar.server.db
  (:refer-clojure :exclude [run!])
  (:require
    [honeysql.core :as sql]
    [honeysql.helpers :as sql-helpers]
    [district0x.server.state :as state]))

(defn log-error [err]
  (when err
    (println err)))

; node CHAR(66) NOT NULL,

(defn run! [db query keys m]
  (.run db query (clj->js ((apply juxt keys) m)) log-error))

(defn create-tables! [db]
  (.serialize db (fn []
                   (.run db "CREATE TABLE offerings (
                          address CHAR(42) PRIMARY KEY NOT NULL,
                          createdOn UNSIGNED INTEGER NOT NULL,
                          name VARCHAR NOT NULL,
                          originalOwner CHAR(42) NOT NULL,
                          offeringType UNSIGNED INTEGER NOT NULL,
                          price UNSIGNED BIG INT NOT NULL,
                          endTime UNSIGNED INTEGER DEFAULT NULL,
                          isNodeOwner BOOLEAN NOT NULL DEFAULT false
                          )" log-error)
                   (.run db "CREATE INDEX createdOnIndex ON offerings (createdOn)" log-error)
                   (.run db "CREATE INDEX nameIndex ON offerings (name)" log-error)
                   (.run db "CREATE INDEX originalOwnerIndex ON offerings (originalOwner)" log-error)
                   (.run db "CREATE INDEX priceIndex ON offerings (price)" log-error)
                   (.run db "CREATE INDEX endTimeIndex ON offerings (endTime)" log-error)
                   (.run db "CREATE INDEX isNodeOwnerIndex ON offerings (isNodeOwner)" log-error)

                   (.run db "CREATE TABLE offeringRequests (
                          node CHAR(66) PRIMARY KEY NOT NULL,
                          requestsCount UNSIGNED INTEGER NOT NULL DEFAULT 0
                          )" log-error)

                   (.run db "CREATE INDEX requestsCountIndex ON offeringRequests (requestsCount)" log-error))))

(defn upsert-offering! [db values]
  (run! db "INSERT OR REPLACE INTO offerings
              (address, createdOn, name, originalOwner, offeringType, price, endTime, isNodeOwner)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)
              "
        [:offering/address
         :offering/created-on
         :offering/name
         :offering/original-owner
         :offering/type
         :offering/price
         :english-auction-offering/end-time
         :offering/node-owner?]
        values))

(defn upsert-offering-requests! [db values]
  (run! db "INSERT OR REPLACE INTO offeringRequests
              (node, requestsCount)
              VALUES (?, ?)
              "
        [:offering-requests/node
         :offering-requests/count]
        values))
