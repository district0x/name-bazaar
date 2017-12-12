(ns district.server.smart-contracts.core
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.utils :refer [js->cljkk camel-case]]
    [cljs.core.async :refer [<! >! timeout]]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [district.server.config.core :refer [config]]
    [district.server.web3.core :refer [web3]]
    [mount.core :as mount :refer [defstate]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(declare start)

(defstate smart-contracts :start (start (merge (:smart-contracts @config)
                                               (:smart-contracts (mount/args)))))

(def fs (nodejs/require "fs"))
(def process (nodejs/require "process"))
(def deasync (nodejs/require "deasync"))


(defn contract [contract-key]
  (get @(:contracts @smart-contracts) contract-key))


(defn contract-address [contract-key]
  (:address (contract contract-key)))


(defn contract-name [contract-key]
  (:name (contract contract-key)))


(defn contract-abi [contract-key]
  (:abi (contract contract-key)))


(defn contract-bin [contract-key]
  (:bin (contract contract-key)))


(declare contract-call)
(declare contract-event-once)


(defn instance
  ([contract-key]
   (:instance (contract contract-key)))
  ([contract-key contract-address]
   (web3-eth/contract-at @web3 (contract-abi contract-key) contract-address)))


(defn update-contract! [contract-key contract]
  (swap! (:contracts @smart-contracts) update contract-key merge contract))


(defn- fetch-contract [file-name & [{:keys [:path]}]]
  (let [path (or path (str (.cwd process) "/resources/public/contracts/build/"))]
    (.readFileSync fs (str path file-name) "utf-8")))


(defn- fetch-abi [contract-name & [opts]]
  (js/JSON.parse (fetch-contract (str contract-name ".abi"))))


(defn- fetch-bin [contract-name & [opts]]
  (fetch-contract (str contract-name ".bin") opts))


(defn load-contract-files [contract {:keys [:contracts-src-path]}]
  (let [fetch-opts {:path contracts-src-path}
        abi (fetch-abi (:name contract) fetch-opts)
        bin (fetch-bin (:name contract) fetch-opts)]
    (merge contract
           {:abi abi
            :bin bin
            :instance (web3-eth/contract-at @web3 abi (:address contract))})))


(defn start [{:keys [:contracts-src-path :contracts-var print-gas-usage?] :as opts}]
  (merge
    {:contracts (atom (into {} (map #(vec [(first %) (load-contract-files (second %) opts)]) @contracts-var)))}
    opts))


(defn link-library [bin placeholder library-address]
  (string/replace bin placeholder (subs library-address 2)))


(defn link-contract-libraries [smart-contracts bin library-placeholders]
  (reduce (fn [bin [replacement placeholder]]
            (let [address (if (keyword? replacement)
                            (get-in smart-contracts [replacement :address])
                            replacement)]
              (link-library bin (string/lower-case placeholder) address)))
          bin library-placeholders))

(defn- handle-deployed-contract! [contract-key contract abi tx-hash]
  (let [{:keys [:gas-used :block-number :contract-address]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        contract (merge contract {:instance (web3-eth/contract-at @web3 abi contract-address)
                                  :address contract-address})]
    (when (and gas-used block-number)
      (update-contract! contract-key contract)
      (when (:print-gas-usage? @smart-contracts)
        (println (:name contract) contract-address (.toLocaleString gas-used)))
      contract)))


(defn- deploy-smart-contract* [{:keys [:contract-key :library-placeholders :args]
                                :as opts} callback]
  (let [{:keys [:abi :bin] :as contract} (load-contract-files (contract contract-key) @smart-contracts)
        opts (merge {:data (str "0x" (link-contract-libraries @(:contracts @smart-contracts) bin library-placeholders))
                     :gas 4000000}
                    (when-not (:from opts)
                      {:from (first (web3-eth/accounts @web3))})
                    opts)
        Contract (apply web3-eth/contract-new @web3 abi (into (vec args) [opts]))
        tx-hash (aget Contract "transactionHash")
        filter-id (atom nil)]

    (if-not (fn? callback)
      (handle-deployed-contract! contract-key contract abi tx-hash)
      (reset!
        filter-id
        (web3-eth/filter
          @web3
          "latest"
          (fn [err]
            (when err
              (callback err))
            (try
              (when-let [contract (handle-deployed-contract! contract-key contract abi tx-hash)]
                (web3-eth/stop-watching! @filter-id)
                ;; reset is needed, otherwise web3 crashes with "Can't connect to" on next request
                ;; Reasons unknown. Needed only because of deasync hack
                (web3/reset @web3)
                (callback nil contract))
              (catch js/Error err
                (callback err)))))))))


(def deploy-smart-contract-deasynced (deasync deploy-smart-contract*))

(defn deploy-smart-contract! [& args]
  (if (:auto-mining? @smart-contracts)
    (apply deploy-smart-contract* args)
    (apply deploy-smart-contract-deasynced args)))


(defn write-smart-contracts! []
  (let [{:keys [:ns :file :name]} (meta (:contracts-var @smart-contracts))]
    (.writeFileSync fs file
                    (str "(ns " ns ") \n\n"
                         "(def " name " \n"
                         (as-> @(:contracts @smart-contracts) $
                               (map (fn [[k v]]
                                      [k (dissoc v :instance :abi :bin)]) $)
                               (into {} $)
                               ;; cljs.pprint/write won't compile with simple optimisations
                               ;; therefore must be required only in dev environment
                               (cljs.pprint/write $ :stream nil))
                         ")"))))


(defn- handle-gas-usage-printing [method tx-hash]
  (let [{:keys [:gas-used :block-number :status :logs]} (web3-eth/get-transaction-receipt @web3 tx-hash)]
    (when (and gas-used block-number)
      (println method (.toLocaleString gas-used) (if (zero? status) "failed" ""))
      gas-used)))


(defn- instance-from-arg [contract]
  (cond
    (keyword? contract) (instance contract)
    (sequential? contract) (instance (first contract) (second contract))
    :else contract))


(defn contract-call [contract method & args]
  "Contract parameter can be one of:
   - keyword :some-contract
   - tuple of keyword and address [:some-contract 0x1234...]
   - instance SomeContract"
  (let [contract (instance-from-arg contract)
        last-arg (last args)
        args (if (and (map? last-arg)
                      (not (:from last-arg)))
               (concat (butlast args) [(merge last-arg {:from (first (web3-eth/accounts @web3))})])
               args)
        result (apply web3-eth/contract-call contract method args)
        filter-id (atom nil)]
    (when (and (:print-gas-usage? @smart-contracts)
               (map? (last args))
               (string? result))
      (if (:auto-mining? @smart-contracts)
        (handle-gas-usage-printing method result)
        (reset! filter-id
                (web3-eth/filter
                  @web3
                  "latest"
                  (fn []
                    (when-let [gas-used (handle-gas-usage-printing method result)]
                      (web3-eth/stop-watching! @filter-id)))))))
    result))


(defn contract-event-in-tx [tx-hash contract event-name & args]
  (let [event-filter (apply web3-eth/contract-call (instance-from-arg contract) event-name args)
        formatter (aget event-filter "formatter")
        event-name-camel (name (camel-case event-name))
        {:keys [:logs]} (web3-eth/get-transaction-receipt @web3 tx-hash)]
    (reduce (fn [result log]
              (let [{:keys [:event] :as evt} (js->clj (formatter (clj->js log)) :keywordize-keys true)]
                (when (= event event-name-camel)
                  (reduced evt))))
            nil
            logs)))

(defn replay-past-events [event-filter callback & [{:keys [:delay :transform-fn]
                                                    :or {delay 0 transform-fn identity}}]]
  (.get event-filter (fn [err all-logs]
                       (when err
                         (throw (js/Error. err)))
                       (let [all-logs (transform-fn (js->cljkk all-logs))]
                         (go-loop [logs all-logs]
                                  (when (seq logs)
                                    (<! (timeout delay))
                                    (callback nil (first logs))
                                    (recur (rest logs))))))))