(ns district0x.server.logging
  (:require [ajax.core :as ajax :refer [GET POST]]
            [cljs-node-io.core :as io :refer [slurp spit]]
            [cljs-node-io.file :refer [File]]
            [clojure.set :as clj-set]
            [clojure.string :as string]
            [district0x.server.state :as state]
            [district0x.shared.encryption-utils :as encryption-utils]
            [taoensso.timbre :as timbre]))

(defn logline [logdata]
  (-> logdata
      (select-keys [:instant :level :ns :message :meta :?file :?line])
      (clj-set/rename-keys {:instant :timestamp})))

(defn- decode-vargs [vargs]
  (reduce (fn [m arg]
            (assoc m (cond
                       (qualified-keyword? arg) :ns
                       (string? arg) :message
                       :else :meta) arg))
          {}
          vargs))

(defn wrap-decode-vargs [data]
  "Middleware for vargs"
  (merge data (decode-vargs (-> data
                                :vargs))))

(defn console-appender []
  {:enabled?   true
   :async?     false
   :min-level  nil
   :rate-limit nil
   :output-fn  nil
   :fn (fn [data]
         (prn (logline data)))})

(defn file-appender
  [{:keys [path] :as options}]
  (let [f (File. path)
        nl "\n"]
    {:enabled?   true
     :async?     false
     :min-level  nil
     :rate-limit nil
     :output-fn  nil
     :fn (fn [data]
           (spit path (str (logline data) nl) :append (.exists f)))}))

(defn setup! [logging-config]
  (timbre/merge-config!
    {:level (-> logging-config
                 :level
                 keyword)
     :middleware [wrap-decode-vargs]
     :appenders {:console (when (:console logging-config) (console-appender))
                 :file (when (:file logging-config) (file-appender {:path (get-in logging-config [:file :path])}))}}))
