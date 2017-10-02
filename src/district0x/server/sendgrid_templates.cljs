(ns district0x.server.sendgrid-templates
  (:require [goog.string :as gstring]
            [goog.string.format :as format]))

(def currencies
  {0 "Ξ"
   1 "$"
   2 "€"
   3 "£"
   4 "\u20BD"
   5 "¥"
   6 "¥"})

(def currency-code->id
  {:ETH 0
   :USD 1
   :EUR 2
   :GBP 3
   :RUB 4
   :CNY 5
   :JPY 6})

(def currency-id->code
  {0 :ETH
   1 :USD
   2 :EUR
   3 :GBP
   4 :RUB
   5 :CNY
   6 :JPY})

(defn format-currency [value currency-code]
  (str value (get currencies (currency-code->id currency-code))))

(defn on-offering-added [name]
  "Offering was created for name requested by user"
  (gstring/format
   "An offering has just been created for a name %s requested by you."
   (gstring/htmlEscape name)))

(defn on-auction-finalized [k name price]
  "Auction was finalized. Both seller and buyer receive email."
  (case

    (= k :owner)
    (gstring/format
     "Your auction %s has just been finalized. The winning bid was <b>%s</b>."
     (gstring/htmlEscape name)
     (format-currency price :ETH))

    (= k :winner)
    (gstring/format
     "Congratulaions! You won the auction %s. The winning bid was <b>%s</b>."
     (gstring/htmlEscape name)
     (format-currency price :ETH))))

(defn on-offering-bought [name price]
  "Seller's Buy Now offering was bought"
  (gstring/format
   "Your Buy Now offering %s has just been bought for <b>%s</b>."
   (gstring/htmlEscape name)
   (format-currency price :ETH)))

(defn on-new-bid [name price]
  "Seller's Auction offering got new bid"
  (gstring/format
   "You auction offering has just got a new bid for <b>%s</b>."
   (format-currency price :ETH)))



