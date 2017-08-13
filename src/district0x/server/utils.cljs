(ns district0x.server.utils
  (:require
    [cljs-web3.async.eth :as web3-eth-async]
    [cljs-web3.async.evm :as web3-evm-async]
    [cljs-web3.eth :as web3-eth]
    [cljs.core.async :refer [<! >! chan]]
    [cljs.core.async.impl.channels]
    [clojure.string :as string]
    [cljs.nodejs :as nodejs])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def fs (js/require "fs"))
(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))

(defn fetch-contract [file-name & [{:keys [:contracts-path :response-format]
                                    :or {contracts-path (str (.cwd js/process)
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

(defn rand-str [n & [{:keys [:lowercase-only?]}]]
  (let [chars-between #(map char (range (.charCodeAt %1) (inc (.charCodeAt %2))))
        chars (concat (when-not lowercase-only? (chars-between \0 \9))
                      (chars-between \a \z)
                      (when-not lowercase-only? (chars-between \A \Z))
                      (when-not lowercase-only? [\_]))
        password (take n (repeatedly #(rand-nth chars)))]
    (reduce str password)))

(defn chan? [x]
  (instance? cljs.core.async.impl.channels/ManyToManyChannel x))