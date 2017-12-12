(ns district.server.db.column-types
  (:require [honeysql.core :as sql]))

(def address (sql/call :char (sql/inline 42)))
(def not-nil (sql/call :not nil))
(def default-nil (sql/call :default nil))
(def default-zero (sql/call :default (sql/inline 0)))
(def default-false (sql/call :default (sql/inline false)))
(def sha3-hash (sql/call :char (sql/inline 66)))
(def primary-key (sql/call :primary-key))
