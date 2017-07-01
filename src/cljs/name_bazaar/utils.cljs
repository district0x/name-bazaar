(ns name-bazaar.utils
  (:require [clojure.string :as string]))

(defn namehash [name]
  (if (empty? name)
    "0x0000000000000000000000000000000000000000000000000000000000000000"
    (let [[label & rest] (string/split name ".")]
      (js/SoliditySha3.sha3 (namehash (string/join "." rest))
                            (js/SoliditySha3.sha3 label)))))