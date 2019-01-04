(ns name-bazaar.server.api
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as string]
    [district.server.config :refer [config]]
    [district.server.endpoints :refer [send reg-get! query-params route-params]]
    [district.shared.error-handling :refer [try-catch]]
    [district0x.shared.utils :refer [combination-of? collify]]
    [name-bazaar.server.db :as db]))

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

(s/def ::offering-requests-order-by (partial contains? #{:offering-request/requesters-count}))

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

(defn parse-offering-requests-params [params]
  (-> params
    (update :name #(when (seq (str %)) (str %)))
    (update :name-position #(when (keyword? %) %))
    (update :limit #(if % (min 100 %) 100))
    (update :order-by #(when (s/valid? ::offering-requests-order-by %) %))
    (update :order-by-dir #(if (s/valid? ::order-by-dir %) % :asc))))

(reg-get! "/offerings"
          (fn [req res]
            (try-catch
             (send res (db/get-offerings (parse-offerings-params (query-params req)))))))

(reg-get! "/offering-requests"
          (fn [req res]
            (try-catch
             (send res (db/get-offering-requests (parse-offering-requests-params (query-params req)))))))

(reg-get! "/config"
          (fn [req res]
            (try-catch
             (send res (:ui @config)))))
