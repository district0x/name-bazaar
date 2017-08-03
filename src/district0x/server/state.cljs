(ns district0x.server.state)

(defonce *server-state* (atom {:log-gas-usage? true
                               :smart-contracts {}
                               :my-addresses []
                               :web3 nil
                               :db nil}))

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
   (active-address @*server-state*))
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
