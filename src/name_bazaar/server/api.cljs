(ns name-bazaar.server.api
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [district0x.server.api-server :as api-server :refer [send-json!]]
    [district0x.server.state :as state]
    [medley.core :as medley]
    [name-bazaar.server.db :as db]
    [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(api-server/reg-get!
  "/offerings"
  (fn [req res]
    (go
      (send-json! res (<! (db/search-offerings
                            (state/db)
                            (api-server/sanitized-query-params req)))))))

(api-server/reg-get!
  "/offering-requests"
  (fn [req res]
    (go
      (send-json! res (<! (db/search-offering-requests
                            (state/db)
                            (api-server/sanitized-query-params req)))))))


