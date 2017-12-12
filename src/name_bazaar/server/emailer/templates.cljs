(ns name-bazaar.server.emailer.templates
  (:require
    [bidi.bidi :as bidi]
    [district0x.shared.utils :refer [wei->eth]]
    [goog.string :as gstring]
    [name-bazaar.shared.constants :refer [routes]]))

(defn form-link [offering]
  (str "\"https://namebazaar.io"
       (bidi/path-for routes :route.offerings/detail :offering/address offering)
       "\""))

(defn format-price [price]
  (str (.toLocaleString (wei->eth price)) " ETH"))

(defn on-offering-added [offering name]
  "Offering was created for name requested by user"
  (gstring/format
    "An <a class=\"link\" href=%s>offering</a> has just been created for a name <b>%s</b> requested by you."
    (form-link offering)
    (gstring/htmlEscape name)))

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
