(ns district.ui.logging
  (:require ["@sentry/browser" :as Sentry]
            [devtools.preload]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as timbre])
  (:require-macros [district.shared.error-handling :refer [error?]]))

(declare start)
(defstate logging :start (start (:logging (mount/args))))

(def ^:private devtools-level->fn
  {:fatal js/console.error,
   :error js/console.error,
   :warn js/console.warn,
   :info js/console.info,
   :debug js/console.info,
   :trace js/console.trace})

(def ^:private timbre->sentry-levels
  {:fatal "fatal"
   :error "error"
   :warn "warning"
   :info "info"
   :debug "debug"
   :trace "debug"
   :report "info"})

(defn- decode-vargs [vargs]
  (reduce (fn [m arg]
            (assoc m (cond
                       (qualified-keyword? arg) :log-ns
                       (string? arg) :message
                       (map? arg) :meta)
                   arg))
          {}
          vargs))

(def devtools-appender
  "Simple js/console appender which avoids pr-str and uses cljs-devtools
  to format output"
  {:enabled? true
   :async? false
   :min-level nil
   :rate-limit nil
   :output-fn nil
   :fn
   (fn [data]
     (let [{:keys [level ?ns-str ?line vargs_]} data
           vargs (list* (str ?ns-str ":" ?line) (force vargs_))
           f (devtools-level->fn level js/console.log)]
       (.apply f js/console (to-array vargs))))})

(defn sentry-appender [{:keys [:min-level]}]
  {:enabled? true
   :async? true
   :min-level (or min-level :warn)
   :rate-limit nil
   :output-fn :inherit
   :fn (fn [{:keys [:level :?ns-str :?line :message :meta :log-ns] :as data}]
         (let [{:keys [:error :user :ns :line]} meta]
           (when meta
             (js-invoke Sentry "configureScope" (fn [scope]
                                                     (doseq [[k v] meta]
                                                       (-> scope (.setExtra (name k) (clj->js v))))
                                                     (when user
                                                       (-> scope (.setUser (clj->js user)))))))
           (if (error? error)
             (js-invoke Sentry "captureException" error)
             (js-invoke Sentry "captureEvent" (clj->js {:level (timbre->sentry-levels level)
                                                           :message (or message error)
                                                           :logger (str (or log-ns ns ?ns-str) ":" (or line ?line))})))))})

(defn wrap-decode-vargs [data]
  "Middleware for vargs"
  (merge data (decode-vargs (:vargs data))))

(defn start [{:keys [:level :console? :sentry]}]
  (when sentry
    (js-invoke Sentry "init" (clj->js sentry)))
  (timbre/merge-config! {:level (keyword level)
                         :middleware [wrap-decode-vargs]
                         :appenders {:console (when console?
                                                devtools-appender)
                                     :sentry (when sentry
                                               (sentry-appender sentry))}}))
