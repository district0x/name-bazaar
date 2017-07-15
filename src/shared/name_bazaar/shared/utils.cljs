(ns name-bazaar.shared.utils
  (:require [district0x.big-number :as bn]))

(defn offering-type->kw [offering-type]
  (if (>= (bn/->number offering-type) 100000)
    :english-auction-offering
    :instant-buy-offering))
