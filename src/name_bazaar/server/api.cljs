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

(api-server/reg-route! :get
                       "/config/:key"
                       (fn [request response]
                         (let [config-key (-> (aget request "params")
                                              (js->clj :keywordize-keys true)
                                              vals
                                              first
                                              keyword)]
                           (if (contains? state/whitelisted-config-keys config-key)
                             (-> response
                                 (api-server/status 200)
                                 (api-server/send (state/config config-key)))
                             (-> response
                                 (api-server/status 400)
                                 (api-server/send "Bad request"))))))

(api-server/reg-route! :get
                       "/config"
                       (fn
                         [request response]
                         (-> response
                             (api-server/status 200)
                             (api-server/send (->> (select-keys (state/config) state/whitelisted-config-keys)
                                                   (api-server/write-transit))))))
