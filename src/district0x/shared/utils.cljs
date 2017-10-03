(ns district0x.shared.utils
  (:require
    [cemerick.url :as url]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
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

(defn replace-comma [x]
  (and (string? x) (string/replace x \, \.)))

(defn eth->wei [x]
  (web3/to-wei (if (string? x) (replace-comma x) x) :ether))

(def eth->wei->num (comp js/parseInt eth->wei))

(defn safe-eth->wei->num [x]
  (when-not (empty? x)
    (try
      (eth->wei x)
      (catch :default e
        nil))))

(def big-num->ether (comp bn/->number wei->eth))

(defn long->epoch [x]
  (/ x 1000))

(defn epoch->long [x]
  (* x 1000))

(def zero-address "0x0000000000000000000000000000000000000000")

(defn zero-address? [x]
  (or (= x zero-address)
      (= x "0x")))

(defn empty-address? [x]
  (or (zero-address? x)
      (not x)))

(defn collify [x]
  (if (sequential? x) x [x]))

(defn empty-string? [x]
  (and (string? x) (empty? x)))

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

(defn pos-ether-value? [x & [opts]]
  (and (non-neg-ether-value? x opts)
       (or (and (string? x)
                (pos? (parse-float x)))
           (and (number? x)
                (pos? x))
           (and (bn/big-number? x)
                (bn/pos? x)))))

(def non-neg-or-empty-ether-value? #(non-neg-ether-value? % {:allow-empty? true}))

(defn eth-props->wei-props [args wei-keys]
  (medley/map-kv (fn [key value]
                   (if (contains? wei-keys key)
                     [key (if (sequential? value)
                            (map eth->wei value)
                            (eth->wei value))]
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

(defn update-multi [m keyseq f]
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

(defn combination-of? [keys-set coll]
  (let [coll (collify coll)]
    (and (seq coll)
         (every? (partial contains? keys-set) coll))))

(letfn [(merge-in* [a b]
          (if (map? a)
            (merge-with merge-in* a b)
            b))]
  (defn merge-in
    "Merge multiple nested maps."
    [& args]
    (reduce merge-in* nil args)))

(defn parse-order-by-search-params [order-by-columns order-by-dirs]
  (map vec (partition-all 2 (interleave (or order-by-columns []) (or order-by-dirs [])))))

(defn split-order-by-search-params [order-by]
  (let [order-by (or order-by [[]])]
    (map vec (split-at (count order-by) (map name (apply interleave order-by))))))

(defn apply-parsers [m parsers]
  (medley/map-kv (fn [k v]
                   (if-let [parser (get parsers k)]
                     [k (parser v)]
                     [k v]))
                 m))

(defn sort-by-desc [key-fn coll]
  (sort-by key-fn #(compare %2 %1) coll))

(defn sort-desc [coll]
  (sort #(compare %2 %1) coll))

(defn rand-str [n & [{:keys [:lowercase-only?]}]]
  (let [chars-between #(map char (range (.charCodeAt %1) (inc (.charCodeAt %2))))
        chars (concat (when-not lowercase-only? (chars-between \0 \9))
                      (chars-between \a \z)
                      (when-not lowercase-only? (chars-between \A \Z)))
        password (take n (repeatedly #(rand-nth chars)))]
    (reduce str password)))

(defn rand-nth-except [exception coll]
  (first (shuffle (remove (partial = exception) coll))))

(defn prepend-address-zeros [address]
  (let [n (- 42 (count address))]
    (if (pos? n)
      (->> (subs address 2)
        (str (string/join (take n (repeat "0"))))
        (str "0x"))
      address)))

