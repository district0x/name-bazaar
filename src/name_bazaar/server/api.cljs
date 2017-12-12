(ns name-bazaar.server.api
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as string]
    [district.server.api-server.core :as api-server :refer [send reg-get! query-params route-params]]
    [district.server.config.core :refer [config]]
    [district0x.shared.utils :refer [combination-of? collify]]
    [name-bazaar.server.db :as db]
    [taoensso.timbre :as logging]))

(defn db-unavailable [res kw]
  (logging/warn "Database is not initialized" kw)
  (send res 503 "Service unavailiable"))

(declare parse-offerings-params)
(declare parse-offering-requests-params)

(reg-get! "/offerings"
          (fn [req res]
            (if-let [database true]
              (send res (db/get-offerings (parse-offerings-params (query-params req))))
              (db-unavailable res ::get-offerings))))

(reg-get! "/offering-requests"
          (fn [req res]
            (if-let [database true]
              (send res (db/get-offering-requests (parse-offering-requests-params (query-params req))))
              (db-unavailable res ::get-offering-requests))))

(reg-get! "/config"
          (fn [req res]
            (api-server/send res (:ui @config))))

(s/def ::offering-order-by (partial contains? #{:offering/price
                                                :offering/created-on
                                                :offering/finalized-on
                                                :auction-offering/end-time
                                                :auction-offering/bid-count
                                                :name-relevance}))

(s/def ::offering-fields (partial combination-of? #{:offering/address
                                                    :offering/node
                                                    :offering/version
                                                    :offering/name}))

(s/def ::order-by-dir (partial contains? #{:asc :desc}))

(defn parse-address [address]
  (when (string? address)
    (string/lower-case address)))

(defn parse-offerings-params [params]
  (-> params
    (update :name #(when (seq (str %)) (str %)))
    (update :min-price #(when (number? %) %))
    (update :max-price #(when (number? %) %))
    (update :min-length #(when (number? %) %))
    (update :max-length #(when (number? %) %))
    (update :name-position #(when (keyword? %) %))
    (update :order-by #(when (s/valid? ::offering-order-by %) %))
    (update :order-by-dir #(if (s/valid? ::order-by-dir %) % :asc))
    (update :fields #(if (s/valid? ::offering-fields %) % [:offering/address]))
    (update :original-owner parse-address)
    (update :bidder parse-address)
    (update :winning-bidder parse-address)
    (update :exclude-winning-bidder parse-address)
    (update :limit #(if % (min 100 %) 100))
    (update :nodes #(when % (collify %)))))

(s/def ::offering-requests-order-by (partial contains? #{:offering-request/requesters-count}))

(defn parse-offering-requests-params [params]
  (-> params
    (update :name #(when (seq (str %)) (str %)))
    (update :name-position #(when (keyword? %) %))
    (update :limit #(if % (min 100 %) 100))
    (update :order-by #(when (s/valid? ::offering-requests-order-by %) %))
    (update :order-by-dir #(if (s/valid? ::order-by-dir %) % :asc))))