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
                               :testrpc-server nil
                               :config nil}))

(defonce default-config {:private-key nil
                         :public-key nil
                         :sendgrid-api-key nil
                         :api-port 6200
                         :testrpc-port 8549
                         :mainnet-port 8545
                         :shortcircuit-node-watchdog? true ;;Shortcircuit it when connected to built-in TestRPC
                         :frontend-url "http://0.0.0.0:4544"
                         :logging {:level :info
                                   :console true
                                   :file {:path "/tmp/district0x.log"}}})

(defonce whitelisted-config-keys ^{:doc "Config keys that are safe to be propagated to the UI"} #{:public-key :frontend-url})

(defn web3
  ([]
   (web3 @*server-state*))
  ([server-state]
   (:web3 server-state)))

(defn db
  ([]
   (db @*server-state*))
  ([server-state]
   (:db server-state)))

(defn active-address
  ([]
   (active-address @*server-state*))
  ([server-state]
   (:active-address server-state)))

(defn my-address
  ([i]
   (my-address @*server-state* i))
  ([server-state i]
   (nth (:my-addresses server-state) i)))

(defn instance
  ([contract-key]
   (instance @*server-state* contract-key))
  ([server-state contract-key]
   (get-in server-state [:smart-contracts contract-key :instance])))

(defn contract-address
  ([contract-key]
   (contract-address @*server-state* contract-key))
  ([server-state contract-key]
   (get-in server-state [:smart-contracts contract-key :address])))

(defn contract
  ([contract-key]
   (contract @*server-state* contract-key))
  ([server-state contract-key]
   (get-in server-state [:smart-contracts contract-key])))

(defn my-addresses
  ([]
   (my-addresses @*server-state*))
  ([server-state]
   (:my-addresses server-state)))

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

(defmethod config :config-key
  [k]
  (get-in @*server-state* [:config k]))

(defmethod config :config-map
  [server-state]
  (get-in server-state [:config]))

(defmethod config :config-map-key
  [server-state k]
  (get-in server-state [:config k]))

(defmethod config :config
  []
  (get-in @*server-state* [:config]))
 
