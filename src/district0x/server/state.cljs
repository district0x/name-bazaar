(ns district0x.server.state)

(defonce *server-state* (atom {:log-contract-calls? true
                               :log-errors? true
                               :smart-contracts {}
                               :my-addresses []
                               :web3 nil
                               :db nil
                               :node-watchdog {:online? false
                                               :timeout 1000
                                               :enabled? false}
                               :web3-requests-queue cljs.core/PersistentQueue.EMPTY
                               :testrpc-server nil
                               :config nil}))

(def default-config {:private-key nil
                     :public-key nil
                     :sendgrid-api-key nil
                     :api-port 6200
                     :testrpc-port 8549
                     :mainnet-port 8545
                     :pushroute-hosts ["beta.namebazaar.io" "namebazaar.io"]
                     :client "http://0.0.0.0:4544"
                     :use-instant-registrar? true
                     :logging {:level :info
                               :console true}})

(def whitelisted-config-keys
  ^{:doc "Config keys that are safe to be propagated to the UI"}
  #{:public-key :client :use-instant-registrar?})

(defn web3 []
  (:web3 @*server-state*))

(defn db []
  (:db @*server-state*))

(defn active-address []
  (:active-address @*server-state*))

(defn my-address [i]
  (nth (:my-addresses @*server-state*) i))

(defn instance [contract-key]
  (get-in @*server-state* [:smart-contracts contract-key :instance]))

(defn contract-address [contract-key]
  (get-in @*server-state* [:smart-contracts contract-key :address]))

(defn contract [contract-key]
  (get-in @*server-state* [:smart-contracts contract-key]))

(defn my-addresses []
  (:my-addresses @*server-state*))

(defn smart-contracts []
  (:smart-contracts @*server-state*))

(defn dispatch-config
  [& args]
  (cond
    (and (map? (first args)) (keyword? (second args)))
    :config-map-key
    
    (keyword? (first args))
    :config-key

    (map? (first args))
    :config-map

    :else
    :config))

(defmulti config dispatch-config)

(defmethod config :config-key [k]
  (get-in @*server-state* [:config k]))

(defmethod config :config-map []
  (get-in @*server-state* [:config]))

(defmethod config :config-map-key [k]
  (get-in @*server-state* [:config k]))

(defmethod config :config []
  (get-in @*server-state* [:config]))
 
