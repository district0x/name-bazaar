(ns district0x.shared.utils
  (:require
    [cemerick.url :as url]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [clojure.string :as string]
    [district0x.shared.big-number :as bn]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]))

(defn address? [x]
  (web3/address? x))

(defn date? [x]
  (instance? goog.date.DateTime x))

(defn wei->eth [x]
  (web3/from-wei x :ether))

(def wei->eth->num (comp js/parseFloat bn/->number wei->eth))

(defn eth->wei [x]
  (web3/to-wei x :ether))

(def eth->wei->num (comp js/parseInt eth->wei))

(def big-num->ether (comp bn/->number wei->eth))

(defn long->epoch [x]
  (/ x 1000))

(defn epoch->long [x]
  (* x 1000))

(defn zero-address? [x]
  (or (= x "0x0000000000000000000000000000000000000000")
      (= x "0x")))

(defn ensure-vec [x]
  (if (sequential? x) x [x]))

(defn empty-string? [x]
  (and (string? x) (empty? x)))

(defn replace-comma [x]
  (string/replace x \, \.))

(defn parse-float [number]
  (if (string? number)
    (js/parseFloat (replace-comma number))
    number))

(defn not-neg? [x]
  (not (neg? x)))

(defn non-neg-ether-value? [x & [{:keys [:allow-empty?]}]]
  (try
    (when (and (not allow-empty?) (empty-string? x))
      (throw (js/Error.)))
    (let [value (web3/to-wei (if (string? x) (replace-comma x) x) :ether)]
      (and
        (or (and (string? value)
                 (not (= "-" (first value))))
            (and (bn/big-number? value)
                 (not (bn/big-number? value))))))
    (catch :default e
      false)))

(defn pos-ether-value? [x & [props]]
  (and (non-neg-ether-value? x props)
       (or (and (string? x)
                (pos? (parse-float x)))
           (and (number? x)
                (pos? x))
           (and (bn/big-number? x)
                (bn/pos? x)))))

(def non-neg-or-empty-ether-value? #(non-neg-ether-value? % {:allow-empty? true}))

(defn num->wei [value]
  (web3/to-wei (if (string? value) (replace-comma value) value) :ether))

(defn eth-props->wei-props [args wei-keys]
  (medley/map-kv (fn [key value]
                   (if (contains? wei-keys key)
                     [key (if (sequential? value)
                            (map num->wei value)
                            (num->wei value))]
                     [key value]))
                 args))

(defn map->vec [m keys-order]
  (mapv (fn [arg-key]
          (if (sequential? arg-key)
            (map #(get m %) arg-key)
            (get m arg-key)))
        keys-order))

(defn map-selected-keys [f keyseq m]
  (let [keyseq (set keyseq)]
    (into {}
          (map (fn [[k v]]
                 (if (contains? keyseq k)
                   (f [k v])
                   [k v]))
               m))))

(defn map-selected-keys-vals [f keyseq m]
  (map-selected-keys #(vec [(first %) (f (second %))]) keyseq m))

(def http-url-pattern #"(?i)^(?:(?:https?)://)(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,}))\.?)(?::\d{2,5})?(?:[/?#]\S*)?$")

(defn http-url? [x & [{:keys [:allow-empty?]}]]
  (if (and allow-empty? (empty? x))
    true
    (when (string? x)
      (boolean (re-matches http-url-pattern x)))))

(defn error? [x]
  (instance? js/Error x))

(defn sha3? [x]
  (and (string? x)
       (= (count x) 66)
       (string/starts-with? x "0x")))

(defn resolve-conformed-spec-or [resolutions-map conformed-data]
  (let [[key value] conformed-data
        resolution (get resolutions-map key)]
    (if (fn? resolution)
      (resolution value)
      value)))

(defn name-with-ns [x]
  (when x
    (str (when-let [n (namespace x)] (str n "/")) (name x))))

