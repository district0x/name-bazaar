(ns district0x.ui.utils
  (:require
    [cemerick.url :as url]
    [cljs-time.coerce :refer [to-date-time to-long to-epoch to-local-date-time]]
    [cljs-time.core :as t :refer [date-time to-default-time-zone]]
    [cljs-time.format :as time-format]
    [clojure.set :as set]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]
    [reagent.core :as r]
    [bidi.bidi :as bidi]))

(defn color-emphasize [& args]
  (apply js/MaterialUIUtils.colorManipulator.emphasize args))

(defn color-lighten [& args]
  (apply js/MaterialUIUtils.colorManipulator.lighten args))

(defn color-darken [& args]
  (apply js/MaterialUIUtils.colorManipulator.darken args))

(defn get-window-width-size [width]
  (cond
    (>= width 1200) 3
    (>= width 1024) 2
    (>= width 768) 1
    :else 0))

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

(defn match-current-location [routes]
  (bidi/match-route routes (current-location-hash)))

(defn url-query-params->form-data
  ([form-field->query-param]
   (url-query-params->form-data (current-url) form-field->query-param))
  ([url form-field->query-param]
   (->> (:query url)
     (medley/map-keys (set/map-invert (medley/map-vals :name form-field->query-param)))
     (medley/remove-keys nil?)
     (map (fn [[k v]]
            (if-let [parser (get-in form-field->query-param [k :parser])]
              {k (parser v)}
              {k v})))
     (into {}))))

(defn create-length-validator
  ([max-length] (create-length-validator 0 max-length))
  ([min-length max-length]
   (if max-length
     (fn [x]
       (<= (or min-length 0)
           (if (string? x) (count (string/trim x)) 0)
           max-length))
     (constantly true))))

(defn pluralize [text count]
  (str text (when (not= count 1) "s")))

(defn time-remaining [from-time to-time]
  (let [milis-difference (- (to-long to-time) (to-long from-time))]
    {:seconds (js/Math.floor (mod (/ milis-difference 1000) 60))
     :minutes (js/Math.floor (mod (/ (/ milis-difference 1000) 60) 60))
     :hours (js/Math.floor (mod (/ milis-difference 3600000) 24))
     :days (js/Math.floor (/ milis-difference 86400000))}))

(defn etherscan-url [address]
  (gstring/format "https://etherscan.io/address/%s" address))

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

(defn format-eth [x]
  (when x
    (.toLocaleString x js/undefined #js {:maximumFractionDigits 3})))

(defn to-locale-string [x max-fraction-digits]
  (when x
    (.toLocaleString x js/undefined #js {:maximumFractionDigits max-fraction-digits})))

(defn format-eth-with-symbol [x]
  (when x
    (str (format-eth x) " ETH")))

(defn format-dnt-with-symbol [x]
  (when x
    (str (format-eth x) " DNT")))

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
  (if (map? props)
    [props children]
    [nil (concat [props] children)]))

(defn create-with-default-props [component default-props]
  (fn [props & children]
    (let [[props children] (parse-props-children props children)]
      (into [] (concat
                 [component (r/merge-props default-props props)]
                 children)))))

(defn current-component-mui-theme []
  (aget (r/current-component) "_reactInternalInstance" "_context" "muiTheme"))

(defn reg-form-sub [form-key f]
  (reg-sub
    form-key
    :<- [:district0x/form form-key]
    :<- [:district0x/form-configs]
    (fn [[form form-configs] [query-id form-id]]
      (f [(merge (get-in form-configs [form-key :default-data])
                 (form form-id))
          form-configs]
         [query-id form-id]))))








