(ns name-bazaar.server.emailer.templates
  (:require
    [bidi.bidi :as bidi]
    [cljs-web3-next.utils :refer [from-wei]]
    [district.server.web3 :refer [web3]]
    [goog.string :as gstring]
    [name-bazaar.shared.constants :refer [routes]]))

(defn form-link [offering]
  (str "\"https://namebazaar.io"
       (bidi/path-for routes :route.offerings/detail :offering/address offering)
       "\""))

(defn format-price [price]
  (str (.toLocaleString (from-wei @web3 price :ether)) " ETH"))

(defn on-auction-finalized [k offering name price]
  "Auction was finalized. Both seller and buyer receive email."
  (case

    (= k :owner)
    (gstring/format
      "Your <a class=\"link\" href=%s>auction</a> of a name <b>%s</b> has just been finalized. The winning bid was <b>%s</b>."
      (form-link offering)
      (gstring/htmlEscape name)
      (format-price price))

    (= k :winner)
    (gstring/format
      "Congratulations! You won the <a class=\"link\" href=%s>auction</a> of a name <b>%s</b>. Your final bid was <b>%s</b>."
      (form-link offering)
      (gstring/htmlEscape name)
      (format-price price))))

(defn on-offering-bought [offering name price]
  "Seller's Buy Now offering was bought"
  (gstring/format
    "Your Buy Now <a class=\"link\" href=%s>offering</a> for a name <b>%s</b> has just been bought for %s."
    (form-link offering)
    (gstring/htmlEscape name)
    (format-price price)))

(defn on-new-bid [offering name price]
  "Seller's Auction offering got new bid"
  (gstring/format
    "Your <a class=\"link\" href=%s>auction</a> of a name <b>%s</b> just got a new bid for <b>%s</b>."
    (form-link offering)
    (gstring/htmlEscape name)
    (format-price price)))
