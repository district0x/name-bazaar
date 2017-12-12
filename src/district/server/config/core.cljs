(ns district.server.config.core
  (:require
    [cljs-node-io.core :as io :refer [slurp spit]]
    [clojure.walk :as walk]
    [cognitect.transit :as transit]
    [mount.core :as mount :refer [defstate]]))

(declare load)
(defstate config :start (load (:config (mount/args))))

(def env js/process.env)

(letfn [(merge-in* [a b]
          (if (map? a)
            (merge-with merge-in* a b)
            b))]
  (defn merge-in
    "Merge multiple nested maps."
    [& args]
    (reduce merge-in* nil args)))

(defn load
  "Load the config overriding the defaults with values from process.ENV (if exist)."
  ([]
   (load {}))
  ([default-config]
   (let [r (transit/reader :json)
         env-config (if-let [path (aget env "CONFIG")]
                      (-> (transit/read r (slurp path))
                        walk/keywordize-keys))]
     (merge-in default-config env-config))))
