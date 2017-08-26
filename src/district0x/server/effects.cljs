(ns district0x.server.effects
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan put!]]
    [cljs.pprint :as pprint]
    [district0x.server.utils :as d0x-server-utils :refer [fetch-abi fetch-bin link-library]]
    [medley.core :as medley]
    [district0x.server.state :as state])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def Web3 (js/require "web3"))
(def TestRPC (js/require "ethereumjs-testrpc"))
(def fs (js/require "fs"))
(def sqlite3 (.verbose (js/require "sqlite3")))

(defn load-smart-contracts! [server-state-atom contracts & [{:keys [:fetch-opts]}]]
  (->> contracts
    (medley/map-vals (fn [{:keys [:name :address] :as contract}]
                       (let [abi (fetch-abi name fetch-opts)
                             bin (fetch-bin name fetch-opts)]
                         (merge contract
                                {:abi (fetch-abi name fetch-opts)
                                 :bin (fetch-bin name fetch-opts)
                                 :instance (web3-eth/contract-at (:web3 @server-state-atom) abi address)}))))
    (swap! server-state-atom update :smart-contracts merge)))

(defn store-smart-contracts! [smart-contracts {:keys [:file-path :namespace]}]
  (let [smart-contracts (medley/map-vals #(dissoc % :instance :abi :bin) smart-contracts)]
    (fs.writeFileSync (str (.cwd js/process) file-path)
                      (str "(ns " namespace ") \n\n"
                           "(def smart-contracts \n"
                           (pprint/write smart-contracts :stream nil)
                           ")"))))

(defn link-contract-libraries [smart-contracts bin library-placeholders]
  (reduce (fn [bin [contract-key placeholder]]
            (let [address (get-in smart-contracts [contract-key :address])]
              (link-library bin placeholder address)))
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
                                          [(merge {:data (link-contract-libraries
                                                           (:smart-contracts @server-state-atom)
                                                           bin
                                                           library-placeholders)
                                                   :gas 4500000}
                                                  opts)])))]
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
  (let [ch (chan)]
    (if port
      (let [server (.server TestRPC (clj->js (merge {:locked false} testrpc-opts)))]
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
  (when-let [db (:db @server-state)]
    (.close db))
  (swap! server-state assoc :db (new sqlite3.Database ":memory:")))


(defn load-my-addresses! [server-state-atom]
  (let [ch (chan)]
    (go
      (let [[err my-addresses] (<! (web3-eth-async/accounts (:web3 @server-state-atom)))]
        (swap! server-state-atom assoc :my-addresses my-addresses)
        (swap! server-state-atom assoc :active-address (first my-addresses))
        (>! ch [err my-addresses])))
    ch))

(defn logged-contract-call! [server-state & [instance method :as args]]
  (let [ch (chan)]
    (go
      (let [[err tx-hash] (<! (apply web3-eth-async/contract-call args))
            [_ tx-receipt] (<! (web3-eth-async/get-transaction-receipt (state/web3 server-state) tx-hash))]
        (if err
          (println method err)
          (when (:log-contract-calls? server-state)
            (println method (.toLocaleString (:gas-used tx-receipt)))))
        (>! ch [err tx-hash])))
    ch))


