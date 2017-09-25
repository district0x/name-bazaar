(ns district0x.shared.config)

(def default-config
  {:PRIVATE_KEY "25615758538fef2b8a65aa7146c273fb17c03b0d73642feac250b7e79d8f06793eb"
   :PUBLIC_KEY "256ebc161b4751583b3718e77bd5bff97027c607daa553385094ce9410ebe7531b422f7b5f2702ba80b53092024ccc63c4a8c96ba7387e063500a58cce0c7b3a3ee"
   :SENDGRID_API_KEY nil
   :api-port 6200})

(def whitelisted-keys ^{:doc "Keys that are safe to be propagated to the UI"} #{:public-key})

(def ^private *config* (atom nil))

(def ^private env js/process.env)

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
                              (keyword k)
                              (aget env k)))
                     {}
                     (js-keys env))]
     (set-config! (merge default-config env-config)))))
