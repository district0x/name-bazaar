(ns district0x.shared.config
  (:require [clojure.string :as string]))

(def default-config
  {:private-key "25615758538fef2b8a65aa7146c273fb17c03b0d73642feac250b7e79d8f06793eb"
   :public-key "256ebc161b4751583b3718e77bd5bff97027c607daa553385094ce9410ebe7531b422f7b5f2702ba80b53092024ccc63c4a8c96ba7387e063500a58cce0c7b3a3ee"
   :sendgrid-api-key nil
   :api-port 6200
   :testrpc-port 8549
   :mainnet-port 8545
   :frontend-url "http://0.0.0.0:4544"})

(def whitelisted-keys ^{:doc "Keys that are safe to be propagated to the UI"} #{:public-key :frontend-url})

(def ^private *config* (atom nil))

(def ^private env js/process.env)

(defn env->cljkk [s]
  (-> s
      (string/lower-case)
      (string/replace "_" "-")
      keyword))

(defn get-config
  ([]
   (deref *config*))
  ([k]
   (-> (deref *config*)
       (get k))))

(defn- set-config!
  [config]
  (reset! *config* config))

(defn load-config!
  "Load the config overriding the defaults with values from process.ENV (if exists)."
  ([]
   load-config! {})
  ([default-config]
   (let [env-config (reduce
                     (fn [coll k]
                       (assoc coll
                              (env->cljkk k)
                              (aget env k)))
                     {}
                     (js-keys env))]
     (set-config! (merge default-config env-config)))))
