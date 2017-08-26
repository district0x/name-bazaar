(ns name-bazaar.ui.utils
  (:require
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.utils :as d0x-ui-utils]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.ui.constants :as constants]))

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

(defn parse-query-params [query-params route-key]
  (d0x-shared-utils/apply-parsers query-params (constants/query-params-parsers route-key)))

(defn path-for [route route-params]
  (d0x-ui-utils/path-for {:route route
                          :route-params route-params
                          :routes constants/routes}))

(defn etherscan-ens-url [name]
  (gstring/format "https://etherscan.io/enslookup?q=%s" name))