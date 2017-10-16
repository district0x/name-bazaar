(ns district0x.ui.utils
  (:require
    [bidi.bidi :as bidi]
    [cemerick.url :as url]
    [cljs-time.coerce :refer [to-date-time to-long to-epoch to-local-date-time]]
    [cljs-time.core :as t :refer [date-time to-default-time-zone]]
    [cljs-time.format :as time-format]
    [clojure.set :as set]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils]
    [goog.format.EmailAddress :as email-address]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]
    [reagent.core :as r]))

(defn get-screen-size [width]
  (cond
    (>= width 1920) 4
    (>= width 1200) 3
    (>= width 992) 2
    (>= width 768) 1
    :else 0))

;; TODO: get whitelisted domains from config
(defn hashroutes? []
  (when-not (contains? #{"beta.namebazaar.io" "namebazaar.io"}
                       (-> js/window
                           .-location
                           .-hostname))
    true))

(defn current-url []
  (url/url (string/replace (.-href js/location) "#" "")))

(defn current-location-hash []
  (let [hash (-> js/document
               .-location
               .-hash
               (string/split #"\?")
               first
               (string/replace "#" ""))]
    (if (empty? hash) "/" hash)))

(defn path-for [{:keys [:route :route-params :routes]}]
  (let [path (medley/mapply bidi/path-for routes route route-params)]
    (if (hashroutes?)
      (str "#" path)
      path)))

(defn match-current-location
  ([routes route]
   (bidi/match-route routes route))
  ([routes]
   (match-current-location routes (current-location-hash))))

(defn assoc-order-by-search-param [{:keys [:search-params/order-by-columns :search-params/order-by-dirs]
                                    :as search-params}]
  (if (and order-by-columns order-by-dirs)
    (-> search-params
      (assoc :search-params/order-by (d0x-shared-utils/parse-order-by-search-params order-by-columns order-by-dirs))
      (dissoc :search-params/order-by-columns :search-params/order-by-dirs))
    search-params))

(defn url-query-params->form-data
  ([form-field->query-param]
   (url-query-params->form-data (current-url) form-field->query-param))
  ([query-params form-field->query-param]
   (->> query-params
     (medley/map-keys (set/map-invert (medley/map-vals :name form-field->query-param)))
     (medley/remove-keys nil?)
     (map (fn [[k v]]
            (if-let [parser (get-in form-field->query-param [k :parser])]
              {k (parser v)}
              {k v})))
     (into {})
     assoc-order-by-search-param)))

(defn valid-length?
  ([s max-length] (valid-length? s max-length 0))
  ([s max-length min-length]
   (and (string? s) (<= (or min-length 0) (count (string/trim s)) max-length))))

(defn valid-email? [s & [{:keys [:allow-empty?]}]]
  (let [valid? (email-address/isValidAddress s)]
    (if allow-empty?
      (or (empty? s) valid?)
      valid?)))

(defn pluralize [text count]
  (str text (when (not= count 1) "s")))

(defn etherscan-url [address & [{:keys [:type]
                                 :or {type :address}}]]
  (gstring/format "https://etherscan.io/%s/%s" (if (= type :address) "address" "tx") address))

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

(defn provides-web3? []
  (boolean (aget js/window "web3")))

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










