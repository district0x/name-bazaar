(ns name-bazaar.utils
  (:require [clojure.string :as string]))

(defn sha3 [& args]
  (apply js/SoliditySha3.sha3 args))

(defn namehash [name]
  (if (empty? name)
    "0x0000000000000000000000000000000000000000000000000000000000000000"
    (let [[label & rest] (string/split name ".")]
      (sha3 (namehash (string/join "." rest))
            (sha3 label)))))