(ns name-bazaar.server.emailer.templates
  (:require [bidi.bidi :as bidi]
            [district0x.server.state :as state]
            [district0x.shared.utils :as shared-utils]
            [goog.string :as gstring]
            [name-bazaar.shared.constants :as shared-constants]))

(defn form-link [offering]
 (let [client (:client state/config)]
    (str "\"" client "/"
         (bidi/path-for shared-constants/routes :route.offerings/detail :offering/address offering)
         "\"")))

(defn on-offering-added [offering name]
  "Offering was created for name requested by user"
  (gstring/format
   "An offering has just been created for a name <a href=%s>%s</a> requested by you."
   (form-link offering)
   (gstring/htmlEscape name)))

(defn on-auction-finalized [k offering name price]
  "Auction was finalized. Both seller and buyer receive email."
  (case

      (= k :owner)
    (gstring/format
     "Your auction <a href=%s>%s</a> has just been finalized. The winning bid was <b>%s</b>."
     (form-link offering)
     (gstring/htmlEscape name)
     (str (shared-utils/wei->eth price) " ETH"))

    (= k :winner)
    (gstring/format
     "Congratulations! You won the auction <a href=%s>%s</a>. You final bid was <b>%s</b>."
     (form-link offering)
     (gstring/htmlEscape name)
     (str (shared-utils/wei->eth price) " ETH"))))

(defn on-offering-bought [offering name price]
  "Seller's Buy Now offering was bought"
  (gstring/format
   "Your Buy Now <a href=%s>offering</a> has just been bought for <b>%s</b>."
   (form-link offering)
   (gstring/htmlEscape name)
   (str (shared-utils/wei->eth price) " ETH")))

(defn on-new-bid [offering name price]
  "Seller's Auction offering got new bid"
  (gstring/format
   "Your <a href=%s>auction</a> just got a new bid for <b>%s</b>."
   (form-link offering)
   (str (shared-utils/wei->eth price) " ETH")))

