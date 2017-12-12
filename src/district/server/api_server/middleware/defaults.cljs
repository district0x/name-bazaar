(ns district.server.api-server.middleware.defaults
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [cognitect.transit :as transit]))

(def transit-reader (transit/reader :json))
(def type-is (nodejs/require "type-is"))

(def cors (nodejs/require "cors"))
(def body-parser (nodejs/require "body-parser"))

(defn transit-body-parser [req resp next]
  (when (type-is req "application/transit+json")
    (aset req "body" (transit/read transit-reader (aget req "body"))))
  (next))

(defn- try-parse-keyword-query-param [x]
  (if (and (string? x) (string/starts-with? x ":"))
    (keyword (subs x 1))
    x))

(defn- try-parse-boolean-query-param [x]
  (condp = x
    "true" true
    "false" false
    x))

(defn- try-parse-numeric-query-param [x]
  (let [num (js/parseFloat x)]
    (if (or (js/isNaN num)
            (not= (str num) x))
      x
      num)))

(declare try-parse-query-param)

(defn- try-parse-sequential-query-param [x]
  (if (sequential? x)
    (mapv try-parse-query-param x)
    x))

(defn try-parse-query-param [x]
  (reduce (fn [acc f]
            (let [parsed-x (f x)]
              (if-not (or (string? parsed-x)
                          (sequential? parsed-x))
                (reduced parsed-x)
                parsed-x)))
          x
          [try-parse-boolean-query-param
           try-parse-numeric-query-param
           try-parse-keyword-query-param
           try-parse-sequential-query-param]))

(defn query-params-parser [req resp next]
  (let [query (->> (js->clj (aget req "query") :keywordize-keys true)
                (map (fn [[k v]] [k (try-parse-query-param v)]))
                (into {}))]
    (aset req "query" query)
    (next)))

(def default-middlwares
  {:middleware/cors (cors)
   :middleware/urlencoded (.urlencoded body-parser #js {:extended true})
   :middleware/text-body-parser-for-transit (.text body-parser #js {:type "application/transit+json"})
   :middleware/transit-body-parser transit-body-parser
   :middleware/query-params-parser query-params-parser})