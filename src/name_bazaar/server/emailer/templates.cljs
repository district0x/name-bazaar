(ns name-bazaar.server.emailer.templates
  (:require [bidi.bidi :as bidi]
            [district0x.server.state :as state]
            [goog.string :as gstring]
            [name-bazaar.shared.constants :as shared-constants]))

(defn- form-link [offering]
  (str "\""
       (state/config :frontend-url)
       "/#"
       (bidi/path-for shared-constants/routes :route.offerings/detail :offering/address offering)
       "\""))

(defn on-offering-added [offering name]
  "Offering was created for name requested by user"
  (gstring/format
   "An <a href=%s>offering</a> has just been created for a name %s requested by you."
   (form-link offering)
   (gstring/htmlEscape name)))

(defn on-auction-finalized [k offering name price]
  "Auction was finalized. Both seller and buyer receive email."
  (case

      (= k :owner)
    (gstring/format
     "Your auction %s has just been finalized. The winning bid was <b>%s</b>. See the <a href=%s>offering</a>."
     (gstring/htmlEscape name)
     (str price " ETH")
     (form-link offering))

    (= k :winner)
    (gstring/format
     "Congratulations! You won the auction %s. The winning bid was <b>%s</b>. See the <a href=%s>offering</a>."
     (gstring/htmlEscape name)
     (str price " ETH")
     (form-link offering))))

(defn on-offering-bought [offering name price]
  "Seller's Buy Now offering was bought"
  (gstring/format
   "Your Buy Now <a href=%s>offering</a> has just been bought for <b>%s</b>."
   (form-link offering)
   (gstring/htmlEscape name)
   (str price " ETH")))

(defn on-new-bid [offering name price]
  "Seller's Auction offering got new bid"
  (gstring/format
   "Your <a href=%s>offering</a> just got a new bid for <b>%s</b>."
   (form-link offering)
   (str price " ETH")))

