(ns name-bazaar.ui.utils
  (:require [clojure.string :as string]))

(defn sha3 [& args]
  (apply js/SoliditySha3.sha3 args))

(defn namehash [name]
  (if (empty? name)
    "0x0000000000000000000000000000000000000000000000000000000000000000"
    (let [[label & rest] (string/split name ".")]
      (sha3 (namehash (string/join "." rest))
            (sha3 label)))))

(defn strip-eth-suffix [s]
  (if (and (string? s) (string/ends-with? s ".eth"))
    (subs s 0 (- (count s) 4))
    s))