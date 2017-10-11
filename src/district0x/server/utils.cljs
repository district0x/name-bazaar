(ns district0x.server.utils
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.async.evm :as web3-evm-async]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan take!]]
    [cljs.core.async.impl.channels]
    [cljs.nodejs :as nodejs]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def fs (js/require "fs"))
(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))
(def process (nodejs/require "process"))

(defn fetch-contract [file-name & [{:keys [:contracts-path :response-format]
                                    :or {contracts-path (str (.cwd process)
                                                             "/resources/public/contracts/build/")}}]]
  (.readFileSync fs (str contracts-path file-name) "utf-8"))

(defn fetch-abi [contract-name & [opts]]
  (js/JSON.parse (fetch-contract (str contract-name ".abi"))))

(defn fetch-bin [contract-name & [opts]]
  (fetch-contract (str contract-name ".bin") opts))

(defn watch-event-once [& args]
  (let [ch (chan)
        event-filter (atom nil)]
    (reset! event-filter (apply web3-eth/contract-call
                                (concat args
                                        [(fn [err res]
                                           (web3-eth/stop-watching! @event-filter
                                                                    (fn []
                                                                      (go (>! ch [err res])))))])))
    ch))

(defn wait [ms]
  (let [ch (chan)]
    (js/setTimeout (fn []
                     (go (>! ch true)))
                   ms)
    ch))

(defn link-library [bin placeholder library-address]
  (string/replace bin placeholder (subs library-address 2)))

(defn ensure-namehash [name node]
  (if name (namehash name) node))

(defn ensure-sha3 [label hash]
  (if label (sha3 name) hash))

(defn chan? [x]
  (instance? cljs.core.async.impl.channels/ManyToManyChannel x))

(defn print-take! [ch]
  (take! ch println))

(def tx-sent? (comp d0x-shared-utils/sha3? second))

(defn clj->json
  [coll]
  (.stringify js/JSON (clj->js coll)))

(defn tx-failed? [tx]
  (not (nil? (first tx))))
