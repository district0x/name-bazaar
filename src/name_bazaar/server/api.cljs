(ns name-bazaar.server.api
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [cljs.nodejs :as nodejs]
    [district0x.server.api-server :as api-server :refer [send-json!]]   
    [district0x.server.state :as state]
    [medley.core :as medley]
    [name-bazaar.server.db :as db]
    [taoensso.timbre :as logging])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(api-server/reg-get!
  "/offerings"
  (fn [req res]
    (let [parsed-req (aget req "parsed")]
      (logging/info "Received request" {:request parsed-req})
      (go
        (try     
          (send-json! res (<! (db/search-offerings
                               (state/db)
                               (api-server/sanitized-query-params req))))
          (catch :default e
            (logging/error "Error handling request" {:error e :request parsed-req})))))))

(api-server/reg-get!
  "/offering-requests"
  (fn [req res]
    (let [parsed-req (aget req "parsed")]
      (logging/info "Received request" {:request parsed-req})
      (go
        (try
          (send-json! res (<! (db/search-offering-requests
                               (state/db)
                               (api-server/sanitized-query-params req))))
          (catch :default e
            (logging/error "Error handling request" {:error e :request parsed-req})))))))

(api-server/reg-route! :get
                       "/config/:key"
                       (fn [req res]
                         (let [parsed-req (aget req "parsed")
                               config-key (-> (aget req "params")
                                                  (js->clj :keywordize-keys true)
                                                  vals
                                                  first
                                                  keyword)]
                           (try
                             (logging/info "Received request" {:request parsed-req})
                             (if (contains? state/whitelisted-config-keys config-key)
                               (-> res
                                   (api-server/status 200)
                                   (api-server/send (state/config config-key)))
                               (-> res
                                   (api-server/status 400)
                                   (api-server/send "Bad request")))
                             (catch :default e
                               (logging/error "Error handling request" {:error e :request parsed-req}))))))

(api-server/reg-route! :get
                       "/config"
                       (fn [req res]
                         (let [parsed-req (aget req "parsed")]
                           (try
                             (logging/info "Received request" {:request parsed-req})
                             (-> res
                                 (api-server/status 200)
                                 (api-server/send (->> (select-keys (state/config) state/whitelisted-config-keys)
                                                       (api-server/write-transit))))
                             (catch :default e
                               (logging/error "Error handling request" {:error e :request parsed-req}))))))
