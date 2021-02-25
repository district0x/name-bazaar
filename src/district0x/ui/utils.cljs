(ns district0x.ui.utils
  (:require
    [bidi.bidi :as bidi]
    [cemerick.url :as url]
    [cljsjs.filesaverjs]
    [cljs.pprint :refer [cl-format]]
    [cljs-time.coerce :refer [to-date-time to-long to-epoch to-local-date-time]]
    [cljs-time.core :as t :refer [date-time to-default-time-zone]]
    [cljs-time.format :as time-format]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils]
    [goog.format.EmailAddress :as email-address]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]
    [reagent.core :as r]))

(defn get-window-size [width]
  (cond
    (>= width 1920) 4
    (>= width 1200) 3
    (>= width 992) 2
    (>= width 768) 1
    :else 0))

(defn current-url []
  (url/url (string/replace (.-href js/location) "#" "")))

(defn current-protocol []
  (-> js/window .-location .-protocol))

(defn current-host []
  (-> js/window .-location .-host))

(defn current-location-hash []
  (let [hash (-> js/document
                 .-location
                 .-hash
                 (string/split #"\?")
                 first
                 (string/replace "#" ""))]
    (if (empty? hash) "/" hash)))

(defn match-current-location
  ([routes route]
   (bidi/match-route routes route))
  ([routes]
   (match-current-location routes (current-location-hash))))

(defn safe-assoc-in
  "Invariant version of assoc-in.
  Returns unchanged map if `ks` path is empty"
  [m ks v]
  (if (get-in m (-> ks drop-last vec))
    (assoc-in m ks v)
    m))

(defn valid-length?
  ([s max-length] (valid-length? s max-length 0))
  ([s max-length min-length]
   (and (string? s) (<= (or min-length 0) (count (string/trim s)) max-length))))

(defn valid-email? [s & [{:keys [:allow-empty?]}]]
  (let [valid? (email-address/isValidAddress s)]
    (if allow-empty?
      (or (empty? s) valid?)
      valid?)))

(defn path-with-query [path query-params-map]
  (str path "?" (url/map->query query-params-map)))

(defn pluralize [text count]
  (str text (when (not= count 1) "s")))

(defn etherscan-url
  ([root-url address {:keys [:type]}]
   (gstring/format (str root-url "/%s/%s") (if (= type :address) "address" "tx") address))
  ([address {:keys [:type]}]
   (etherscan-url "https://etherscan.io" address {:type type}))
  ([address]
   (etherscan-url "https://etherscan.io" address {:type :address})))

(defn etherscan-tx-url [tx-hash]
  (etherscan-url tx-hash {:type :transaction}))

(defn bool->yes|no [x]
  (if x "yes" "no"))

(defn format-metric [x]
  (cond
    (< x 1000) (js/parseInt x)
    (< 999 x 1000000) (gstring/format "%.2fK" (/ x 1000))
    (< 999999 x) (gstring/format "%.2fM" (/ x 1000000))))

(defn format-datetime [date]
  (when date
    (time-format/unparse (time-format/formatters :rfc822) date)))

(defn format-local-datetime [date]
  (when date
    (time-format/unparse-local (time-format/formatters :rfc822) date)))

(defn format-local-date [date]
  (when date
    (time-format/unparse-local (time-format/formatter "EEE, dd MMM yyyy Z") date)))

(defn format-date [date]
  (when date
    (time-format/unparse-local (time-format/formatter "EEE, dd MMM yyyy") date)))

(defn format-iso8601 [date]
  (when date
    (time-format/unparse (time-format/formatters :basic-date-time-no-ms) date)))

(defn format-eth [x]
  (when x
    (.toLocaleString x js/undefined #js {:maximumFractionDigits 2})))

(defn to-locale-string [x max-fraction-digits]
  (when x
    (.toLocaleString x js/undefined #js {:maximumFractionDigits max-fraction-digits})))

(defn format-eth-with-code [x]
  (when x
    (str (format-eth x) " ETH")))

(defn format-dnt-with-code [x]
  (when x
    (str (format-eth x) " DNT")))

(defn time-duration-units [milis]
  {:seconds (js/Math.floor (mod (/ milis 1000) 60))
   :minutes (js/Math.floor (mod (/ (/ milis 1000) 60) 60))
   :hours (js/Math.floor (mod (/ milis 3600000) 24))
   :days (js/Math.floor (/ milis 86400000))})

(defn time-remaining [from-time to-time]
  (let [milis-difference (- (to-long to-time) (to-long from-time))]
    (if (pos? milis-difference)
      (time-duration-units milis-difference)
      {:seconds 0 :minutes 0 :hours 0 :days 0})))

(defn time-biggest-unit [{:keys [:seconds :minutes :hours :days]}]
  (cond
    (pos? days) [:days days]
    (pos? hours) [:hours hours]
    (pos? minutes) [:minutes minutes]
    :else [:seconds seconds]))

(defn time-remaining-biggest-unit [from-time to-time]
  (time-biggest-unit (time-remaining from-time to-time)))

(def time-unit->text {:days "day" :hours "hour" :minutes "minute" :seconds "second"})

(def time-unit->short-text {:days "day" :hours "hour" :minutes "min." :seconds "sec."})

(defn format-time-duration-unit [unit amount]
  (str amount " " (pluralize (time-unit->text unit) amount)))

(defn format-time-remaining-biggest-unit
  ([to-time]
   (format-time-remaining-biggest-unit (t/now) to-time))
  ([from-time to-time]
   (let [[unit amount] (time-remaining-biggest-unit from-time to-time)]
     (format-time-duration-unit unit amount))))

(defn format-time-duration-units [milis]
  (let [{:keys [:seconds :minutes :hours :days]} (time-duration-units milis)]
    ;; To ensure proper order
    (.slice
      (reduce (fn [acc [unit amount]]
                (if (pos? amount)
                  (str acc (format-time-duration-unit unit amount) " ")
                  acc))
              ""
              [[:days days] [:hours hours] [:minutes minutes] [:seconds seconds]])
      0 -1)))

(defn truncate
  "Truncate a string with suffix (ellipsis by default) if it is
   longer than specified length."
  ([string length]
   (truncate string length "..."))
  ([string length suffix]
   (let [string-len (count string)
         suffix-len (count suffix)]
     (if (<= string-len length)
       string
       (str (subs string 0 (- length suffix-len)) suffix)))))

(defn left-pad
  ([s len] (left-pad s len " "))
  ([s len ch]
   (cl-format nil (str "~" len ",'" ch "d") (str s))))

(defn remove-0x [s]
  (string/replace s #"0x" ""))

(defn solidity-sha3 [& args]
  (web3/sha3 (string/join ""
                          (map (fn [arg]
                                 (cond
                                   (and (string? arg) (string/starts-with? arg "0x"))
                                   (remove-0x arg)

                                   (string? arg)
                                   (remove-0x (web3/to-hex arg))

                                   (number? arg)
                                   (left-pad (remove-0x (web3/to-hex arg)) 64 "0")))
                               args))
             {:encoding "hex"}))

(defn parse-props-children [props children]
  (if (or (map? props) (nil? props))
    [props children]
    [nil (concat [props] children)]))

(defn create-with-default-props [component default-props]
  (fn [props & children]
    (let [[props children] (parse-props-children props children)]
      (into [] (concat
                 [component (r/merge-props default-props props)]
                 children)))))

(defn time-ago [time]
  (when time
    (let [units [{:name "second" :limit 60 :in-second 1}
                 {:name "minute" :limit 3600 :in-second 60}
                 {:name "hour" :limit 86400 :in-second 3600}
                 {:name "day" :limit 604800 :in-second 86400}
                 {:name "week" :limit 2629743 :in-second 604800}
                 {:name "month" :limit 31556926 :in-second 2629743}
                 {:name "year" :limit nil :in-second 31556926}]
          diff (t/in-seconds (t/interval time (t/now)))]
      (if (< diff 5)
        "just now"
        (let [unit (first (drop-while #(or (>= diff (:limit %))
                                           (not (:limit %)))
                                      units))]
          (-> (/ diff (:in-second unit))
              js/Math.floor
              int
              (#(str % " " (:name unit) (when (> % 1) "s") " ago"))))))))


(defn date+time->local-date-time [date time]
  (t/local-date-time (.getFullYear date) (inc (.getMonth date)) (.getDate date)
                     (.getHours time) (.getMinutes time) (.getSeconds time)))

(defn parse-boolean-string [s]
  (when (string? s)
    (condp = (string/lower-case s)
      "true" true
      "false" false
      nil)))

(defn parse-int-or-nil [x]
  (let [x (js/parseInt x)]
    (when-not (js/isNaN x)
      x)))

(defn parse-float-or-nil [x]
  (let [x (js/parseFloat (d0x-shared-utils/replace-comma x))]
    (when-not (js/isNaN x)
      x)))

(defn parse-kw-coll-query-params [x]
  (mapv keyword (d0x-shared-utils/collify x)))

(defn str-keyword->keyword [x]
  (when (string/starts-with? x ":")
    (keyword (subs x 1))))

(defn namehash [name]
  (js/EthEnsNamehash.hash name))

(def prerender-user-agent?
  (let [agent (.-userAgent (.-navigator js/window))]
    (not (= (.indexOf agent "prerendercloud") -1))))

(defn handle-file-read [evt callback]
  (let [target (.-currentTarget evt)
        file (-> target .-files (aget 0))
        reader (js/FileReader.)]
    (set! (.-value target) "")
    (set! (.-onload reader) (fn [e]
                              (callback (-> e .-target .-result))))
    (.readAsText reader file)))

(defn file-write [filename content & [mime-type]]
  (js/saveAs (new js/Blob
                  (clj->js [content])
                  (clj->js {:type (or mime-type (str "application/json;charset=UTF-8"))}))
             filename))
