(ns district0x.server.effects
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-node-io.core :as io :refer [slurp spit]]
    [cljs-node-io.file :refer [File]]
    [cljs.core.async :refer [<! >! chan put!]]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [clojure.walk :as walk]
    [cognitect.transit :as transit]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils :refer [fetch-abi fetch-bin link-library]]
    [district0x.shared.utils :as d0x-shared-utils]
    [medley.core :as medley])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def Web3 (js/require "web3"))
(def fs (js/require "fs"))
(def process (nodejs/require "process"))
(def env js/process.env)
 
(defn load-config!
  "Load the config overriding the defaults with values from process.ENV (if exist)."
  ([server-state-atom]
   load-config! {})
  ([server-state-atom default-config]
   (let [r (transit/reader :json)
         env-config (if-let [path (aget env "CONFIG")]
                      (-> (transit/read r (slurp path))
                          (walk/keywordize-keys)))]
     (swap! server-state-atom
            (fn [old new] (assoc-in old [:config] new))
            (d0x-shared-utils/merge-in default-config env-config)))))

(defn load-smart-contracts! [server-state-atom contracts & [{:keys [:fetch-opts]}]]
  (->> contracts
    (medley/map-vals (fn [{:keys [:name :address] :as contract}]
                       (let [abi (fetch-abi name fetch-opts)
                             bin (when goog.DEBUG (fetch-bin name fetch-opts))]
                         (merge contract
                                {:abi abi
                                 :bin bin
                                 :instance (web3-eth/contract-at (:web3 @server-state-atom) abi address)}))))
    (swap! server-state-atom update :smart-contracts merge)))

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

(defn deploy-smart-contract! [server-state-atom {:keys [:contract-key :args :contracts-file-path
                                                        :contracts-file-namespace :from-index
                                                        :library-placeholders]
                                                 :as opts}]
  (let [ch (chan)
        {:keys [:abi :bin]} (state/contract @server-state-atom contract-key)
        opts (if from-index (assoc opts :from (state/my-address @server-state-atom from-index))
                            opts)]
    (go
      (let [deploy-ch (chan 2)
            [err Instance] (<! (apply web3-eth-async/contract-new
                                      deploy-ch
                                      (:web3 @server-state-atom)
                                      abi
                                      (into (vec args)
                                            [(-> {:data (str "0x" (link-contract-libraries
                                                                    (:smart-contracts @server-state-atom)
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
                (when (:log-contract-calls? @server-state-atom)
                  (println "Contract" contract-key "deployed at:" address))
                (swap! server-state-atom update-in [:smart-contracts contract-key] merge {:address address
                                                                                          :instance Instance})
                (>! ch address)))))))
    ch))

(defn create-web3! [server-state-atom & [{:keys [:port :url]}]]
  (let [web3
        (if (or port url)
          (web3/create-web3 Web3 (if url
                                   url
                                   (str "http://localhost:" port)))
          (new Web3))]
    (swap! server-state-atom assoc :web3 web3)
    web3))

(defn start-testrpc! [server-state-atom & [{:keys [:port :web3] :as testrpc-opts}]]
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
        (swap! server-state-atom assoc :testrpc-server server))
      (do
        (.setProvider web3 (.provider TestRPC (clj->js (merge {:locked false} testrpc-opts))))
        (put! ch web3)))
    ch))

(defn create-db! [server-state]
  (let [sqlite3 (if goog.DEBUG
                  (.verbose (js/require "sqlite3"))
                  (js/require "sqlite3"))]
    (when-let [db (:db @server-state)]
      (.close db))
    (swap! server-state assoc :db (new sqlite3.Database ":memory:"))))


(defn load-my-addresses! [server-state-atom]
  (let [ch (chan)]
    (go
      (let [[err my-addresses] (<! (web3-eth-async/accounts (:web3 @server-state-atom)))]
        (swap! server-state-atom assoc :my-addresses my-addresses)
        (swap! server-state-atom assoc :active-address (first my-addresses))
        (>! ch [err my-addresses])))
    ch))

(defn logged-contract-call! [server-state & [instance method :as args]]
  (let [ch (chan)
        log-errors? (:log-errors? server-state)]
    (go
      (let [[err tx-hash] (<! (apply web3-eth-async/contract-call args))
            filter-id (atom nil)]

        (if err
          (do
            (when log-errors?
              (println method err))
            (>! ch [err tx-hash]))
          (reset!
            filter-id
            (web3-eth/filter
              (state/web3 server-state)
              "latest"
              (fn [err]
                (when (and log-errors? err)
                  (println err))
                (go
                  (let [[err tx-receipt] (<! (web3-eth-async/get-transaction-receipt (state/web3 server-state) tx-hash))]
                    (when (and log-errors? err)
                      (println method err))
                    (when (and (:gas-used tx-receipt)
                               (:block-number tx-receipt))
                      (when (:log-contract-calls? server-state)
                        (println method (.toLocaleString (:gas-used tx-receipt))))
                      (web3-eth/stop-watching! @filter-id (fn [err] (when err (println err))))
                      (>! ch [err tx-hash]))))))))))
    ch))

