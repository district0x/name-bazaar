(ns name-bazaar.server.api
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [district0x.server.rest-server :as rest-server :refer [send-json!]]
    [district0x.server.state :as state]
    [medley.core :as medley]
    [name-bazaar.server.db :as db]
    [clojure.string :as string]
    [district0x.utils :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(rest-server/reg-get!
  "/offerings"
  (fn [req res]
    (go
      (send-json! res (<! (db/search-offerings (state/db)
                                               (rest-server/sanitized-query-params req)))))))

(rest-server/reg-get!
  "/offering-requests"
  (fn [req res]
    (go
      (send-json! res (<! (db/search-offering-requests (state/db)
                                                       (rest-server/sanitized-query-params req)))))))

