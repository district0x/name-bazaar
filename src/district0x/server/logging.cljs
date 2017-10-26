(ns district0x.server.logging
  (:require [ajax.core :as ajax :refer [GET POST]]
            [cljs-node-io.core :as io :refer [slurp spit]]
            [cljs-node-io.file :refer [File]]
            [clojure.set :as clj-set]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [district0x.server.state :as state]
            [district0x.shared.encryption-utils :as encryption-utils]
            [cljs.nodejs :as nodejs]
            [taoensso.timbre :as timbre]))

(def chalk (nodejs/require "chalk"))

(defn console-logline [logdata]
  (-> logdata
      (select-keys [:instant :level :ns :message :meta :?file :?line])
      (clj-set/rename-keys {:instant :timestamp}))
  (string/join " "
               [((.keyword chalk (case (:level logdata)
                                   :info "cyan"
                                   :warn "yellow"
                                   :error "red"
                                   "red"))
                 (.bold chalk (string/upper-case (name (:level logdata)))))
                (.bold chalk (str (:message logdata)
                                  (if-let [md (:meta logdata)]
                                    (.reset chalk (str "\n" (with-out-str (pprint/pprint md)))))))
                (.bold chalk "in")
                (if (get-in logdata [:meta :raw-error])
                  (str
                   (.reset chalk (get-in logdata [:meta :ns]))
                   "["
                   (get-in logdata [:meta :file])
                   ":"
                   (get-in logdata [:meta :line])
                   "]")
                  (str
                   (.reset chalk (:ns logdata))
                   "["
                   (:?file logdata)
                   ":"
                   (:?line logdata)
                   "]"))
                (.bold chalk "at")
                (.white chalk (:instant logdata))]))

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
         (print (console-logline data)))})

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
