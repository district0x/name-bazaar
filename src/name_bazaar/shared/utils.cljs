(ns name-bazaar.shared.utils
  (:require
    [clojure.string :as string]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [zero-address? zero-address]]
    [cljs-web3.core :as web3]))

(defn offering-version->type [version]
  (if (>= (bn/->number version) 100000)
    :auction-offering
    :buy-now-offering))

(defn name-label [s]
  (first (string/split s ".")))

(defn contains-number? [s]
  (boolean (re-matches #".*[0-9].*" (name-label s))))

(defn contains-special-char? [s]
  (not (boolean (re-matches #"[a-z0-9A-Z]*" (name-label s)))))

(defn contains-non-ascii? [s]
  (not (boolean (re-matches #"[ -~]*" (name-label s)))))

(defn name-level [s]
  (count (re-seq #"\." s)))

(defn top-level-name? [name]
  (and name (= 1 (name-level name))))

(def offering-props [:offering/offering-registry :offering/registrar :offering/node :offering/name :offering/label-hash
                     :offering/original-owner :offering/emergency-multisig :offering/version :offering/created-on
                     :offering/new-owner :offering/price])

(defn parse-offering [offering-address offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when offering
    (let [offering (zipmap offering-props offering)
          offering-type (offering-version->type (:offering/version offering))
          label (name-label (:offering/name offering))]
      (-> offering
        (assoc :offering/address offering-address)
        (update :offering/version bn/->number)
        (assoc :offering/type offering-type)
        (assoc :offering/auction? (= offering-type :auction-offering))
        (assoc :offering/buy-now? (= offering-type :buy-now-offering))
        (update :offering/price (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/->number))
        (update :offering/created-on (if parse-dates? bn/->date-time bn/->number))
        (update :offering/new-owner #(when-not (d0x-shared-utils/zero-address? %) %))
        (assoc :offering/name-level (name-level (:offering/name offering)))
        (assoc :offering/top-level-name? (top-level-name? (:offering/name offering)))
        (assoc :offering/label label)
        (assoc :offering/label-length (count label))
        (assoc :offering/contains-number? (contains-number? (:offering/name offering)))
        (assoc :offering/contains-special-char? (contains-special-char? (:offering/name offering)))
        (assoc :offering/contains-non-ascii? (contains-non-ascii? (:offering/name offering)))))))

(def auction-offering-props [:auction-offering/end-time :auction-offering/extension-duration
                             :auction-offering/min-bid-increase :auction-offering/winning-bidder
                             :auction-offering/bid-count])

(defn parse-auction-offering [auction-offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when auction-offering
    (-> (zipmap auction-offering-props auction-offering)
      (update :auction-offering/end-time (if parse-dates? bn/->date-time bn/->number))
      (update :auction-offering/winning-bidder #(when-not (zero-address? %) %))
      (update :auction-offering/extension-duration bn/->number)
      (update :auction-offering/min-bid-increase (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/->number))
      (update :auction-offering/bid-count bn/->number))))

(def offering-request-props [:offering-request/name :offering-request/requesters-count])

(defn parse-offering-request [offering-request]
  (when offering-request
    (-> (zipmap offering-request-props offering-request)
      (update :offering-request/requesters-count bn/->number))))

(def registrar-entry-states
  {0 :registrar.entry.state/open
   1 :registrar.entry.state/auction
   2 :registrar.entry.state/owned
   3 :registrar.entry.state/forbidden
   4 :registrar.entry.state/reveal
   5 :registrar.entry.state/not-yet-available})

(def registrar-entry-props [:registrar.entry/state :registrar.entry.deed/address :registrar.entry/registration-date
                            :registrar.entry/value :registrar.entry/highest-bid])

(defn parse-registrar-entry [entry & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when entry
    (-> (zipmap registrar-entry-props entry)
      (update :registrar.entry.deed/address #(if (= % "0x") zero-address %))
      (update :registrar.entry/state (comp registrar-entry-states bn/->number))
      (update :registrar.entry/registration-date (if parse-dates? bn/->date-time bn/->number))
      (update :registrar.entry/value bn/->number)
      (update :registrar.entry/highest-bid (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/->number)))))

(defn calculate-min-bid
  ([price min-bid-increase bid-count]
   (calculate-min-bid price min-bid-increase bid-count 0))
  ([price min-bid-increase bid-count pending-returns]
   (let [min-bid-increase (if (pos? bid-count) min-bid-increase 0)]
     (bn/->number (bn/- (bn/+ (web3/to-big-number price) min-bid-increase) pending-returns)))))

(def emergency-state-new-owner "0x000000000000000000000000000000000000dead")



