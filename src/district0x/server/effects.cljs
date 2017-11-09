(ns district0x.server.effects
  (:require
    [cljs-node-io.core :as io :refer [slurp spit]]
    [cljs-node-io.file :refer [File]]
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan put!]]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [clojure.walk :as walk]
    [cognitect.transit :as transit]
    [district0x.server.state :as state :refer [*server-state*]]
    [district0x.server.utils :as d0x-server-utils :refer [fetch-abi fetch-bin link-library]]
    [district0x.shared.utils :as d0x-shared-utils]
    [medley.core :as medley]
    [taoensso.timbre :refer-macros [log trace debug info warn error]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def Web3 (js/require "web3"))
(def fs (js/require "fs"))
(def process (nodejs/require "process"))
(def env js/process.env)

(defn load-config!
  "Load the config overriding the defaults with values from process.ENV (if exist)."
  ([]
   (load-config! {}))
  ([default-config]
   (let [r (transit/reader :json)
         env-config (if-let [path (aget env "CONFIG")]
                      (-> (transit/read r (slurp path))
                        (walk/keywordize-keys)))]
     (swap! *server-state*
            (fn [old new] (assoc-in old [:config] new))
            (d0x-shared-utils/merge-in default-config env-config)))))

(defn load-smart-contracts! [contracts & [{:keys [:fetch-opts]}]]
  (->> contracts
    (medley/map-vals (fn [{:keys [:name :address] :as contract}]
                       (let [abi (fetch-abi name fetch-opts)
                             bin (when goog.DEBUG (fetch-bin name fetch-opts))]
                         (merge contract
                                {:abi abi
                                 :bin bin
                                 :instance (web3-eth/contract-at (state/web3) abi address)}))))
    (swap! *server-state* update :smart-contracts merge)))

(defn store-smart-contracts! [smart-contracts {:keys [:file-path :namespace]}]
  (let [smart-contracts (medley/map-vals #(dissoc % :instance :abi :bin) smart-contracts)]
    (.writeFileSync fs (str (.cwd process) file-path)
                    (str "(ns " namespace ") \n\n"
                         "(def smart-contracts \n"
                         (cljs.pprint/write smart-contracts :stream nil)
                         ")"))))

(defn link-contract-libraries [smart-contracts bin library-placeholders]
  (reduce (fn [bin [replacement placeholder]]
            (let [address (if (keyword? replacement)
                            (get-in smart-contracts [replacement :address])
                            replacement)]
              (link-library bin (string/lower-case placeholder) address)))
          bin library-placeholders))

(defn deploy-smart-contract! [{:keys [:contract-key :args :contracts-file-path
                                      :contracts-file-namespace :from-index
                                      :library-placeholders]
                               :as opts}]
  (let [ch (chan)
        {:keys [:abi :bin]} (state/contract contract-key)
        opts (if from-index (assoc opts :from (state/my-address from-index))
                            opts)]
    (go
      (let [deploy-ch (chan 2)
            [err Instance] (<! (apply web3-eth-async/contract-new
                                      deploy-ch
                                      (state/web3)
                                      abi
                                      (into (vec args)
                                            [(-> {:data (str "0x" (link-contract-libraries
                                                                    (state/smart-contracts)
                                                                    bin
                                                                    library-placeholders))
                                                  :gas 3000000}
                                               (merge opts)
                                               (select-keys [:from :to :gas-price :gas :value :data]))])))]
        (if err
          (println "Error deploying contract" contract-key err)
          (let [[err Instance] (<! deploy-ch)]              ;; Contract address is obtained only on second callback
            (if err
              (println "Error deploying contract" contract-key err)
              (let [address (aget Instance "address")]
                (when (:log-contract-calls? @*server-state*)
                  (println "Contract" contract-key "deployed at:" address))
                (swap! *server-state* update-in [:smart-contracts contract-key] merge {:address address
                                                                                       :instance Instance})
                (>! ch address)))))))
    ch))

(defn create-web3! [& [{:keys [:port :url]}]]
  (let [web3
        (if (or port url)
          (web3/create-web3 Web3 (if url
                                   url
                                   (str "http://localhost:" port)))
          (new Web3))]
    (swap! *server-state* assoc :web3 web3)
    web3))

(defn start-testrpc! [{:keys [:port :web3] :as testrpc-opts}]
  (let [ch (chan)
        TestRPC (js/require "ethereumjs-testrpc")]
    (if port
      (let [server (.server TestRPC
                            (clj->js (merge {:locked false} testrpc-opts)))]
        (.listen server port (fn [err]
                               (if err
                                 (println err)
                                 (do
                                   (println "TestRPC started at port" port)
                                   (put! ch server)))))
        (swap! *server-state* assoc :testrpc-server server))
      (do
        (.setProvider web3 (.provider TestRPC (clj->js (merge {:locked false} testrpc-opts))))
        (put! ch web3)))
    ch))

(defn create-db! []
  (let [ch (chan)
        sqlite3 (if goog.DEBUG
                  (.verbose (js/require "sqlite3"))
                  (js/require "sqlite3"))]
    (if-let [db (state/db)]
      (.close db (fn [err]
                   (when err
                     (error "Error closing database"))
                   (swap! *server-state* assoc :db (new sqlite3.Database ":memory:"))
                   (go
                     (>! ch @*server-state*))))
      (go
        (swap! *server-state* assoc :db (new sqlite3.Database ":memory:"))
        (>! ch @*server-state*)))
    ch))


(defn load-my-addresses! []
  (let [ch (chan)]
    (go
      (let [[err my-addresses] (<! (web3-eth-async/accounts (state/web3)))]
        (swap! *server-state* assoc :my-addresses my-addresses)
        (swap! *server-state* assoc :active-address (first my-addresses))
        (>! ch [err my-addresses])))
    ch))

(defn logged-contract-call! [instance method & args]
  (let [ch (chan)
        log-errors? (:log-errors? @*server-state*)]
    (go
      (let [[err tx-hash] (<! (apply web3-eth-async/contract-call instance method args))
            filter-id (atom nil)]
        (if err
          (do
            (when log-errors?
              (println method err))
            (>! ch [err tx-hash]))
          (reset!
            filter-id
            (web3-eth/filter
              (state/web3)
              "latest"
              (fn [err]
                (when (and log-errors? err)
                  (println err))
                (go
                  (let [[err tx-receipt] (<! (web3-eth-async/get-transaction-receipt (state/web3) tx-hash))]
                    (when (and log-errors? err)
                      (println method err))
                    (when (and (:gas-used tx-receipt)
                               (:block-number tx-receipt))
                      (when (:log-contract-calls? @*server-state*)
                        (println method (.toLocaleString (:gas-used tx-receipt))))
                      (web3-eth/stop-watching! @filter-id (fn [err] (when err (println err))))
                      (>! ch [err tx-hash]))))))))))
    ch))

(defn- process-queued-contract-call! []
  (when-let [[ch & args] (peek (:web3-requests-queue @*server-state*))]
    (apply web3-eth/contract-call (concat args
                                          [(fn [err res]
                                             (go
                                               (>! ch [err res])
                                               (swap! *server-state* update :web3-requests-queue pop)
                                               (process-queued-contract-call!)))]))))

(defn queue-contract-call! [& args]
  (let [args (if (instance? cljs.core.async.impl.channels/ManyToManyChannel (first args))
               args
               (concat [(chan)] args))]
    (swap! *server-state* update :web3-requests-queue conj args)
    (when (= 1 (count (:web3-requests-queue @*server-state*)))
      (process-queued-contract-call!))
    (first args)))

