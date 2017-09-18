(ns name-bazaar.ui.events.events-utils
  (:require
   [clojure.data :as data]))

(defn- debounce?
  "if the things-in-both part of the diff does not contain at least on key that means that key is different in compared maps."
  [old new ks]
  (let [contains-every? (fn [m ks]
                          (every? #(contains? m %) ks))]  
    (-> (clojure.data/diff old new)
        last
        keys
        (contains-every? ks))))
