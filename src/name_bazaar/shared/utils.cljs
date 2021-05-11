(ns name-bazaar.shared.utils
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs.core.match :refer-macros [match]]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils :refer [zero-address? zero-address evm-time->local-date-time jsobj->clj]]))

(def emergency-state-new-owner "0x000000000000000000000000000000000000dead")
(def unregistered-new-owner "0x00000000000000000000000000000000deaddead")
(def unregistered-price-wei 63466346)
(def buy-now-offering-unregister-min-version 2)
(def auction-offering-unregister-min-version 100001)

(def offering-props [:offering/node :offering/name :offering/label-hash :offering/original-owner
                     :offering/new-owner :offering/price :offering/version :offering/created-on
                     :offering/finalized-on])

(defn offering-version->type [version]
  (if (>= (bn/number version) 100000)
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

(defn normalize [name]
  ((if (exists? js/EthEnsNamehash)
     js/EthEnsNamehash.normalize
     (aget (js/require "eth-ens-namehash") "normalize")) name))

(defn normalized? [name]
  (try
    (= (normalize name)
       name)
    (catch js/Error e
      false)))

(defn valid-ens-name? [name]
  (try
    (normalize name)
    true
    (catch js/Error e
      false)))

(defn offering-supports-unregister? [offering-type offering-version]
  (match [offering-type]
         [:auction-offering] (>= offering-version auction-offering-unregister-min-version)
         [:buy-now-offering] (>= offering-version buy-now-offering-unregister-min-version)))

(defn unregistered? [supports-unregister? offering]
  (let [new-owner (:offering/new-owner offering)
        price (-> offering :offering/price bn/number)]
    (match [supports-unregister? (= unregistered-new-owner new-owner) (= unregistered-price-wei price)]
           [true true _] true
           [false _ true] true
           :else false)))

(defn parse-offering [offering-address offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when offering
    (let [offering (select-keys (jsobj->clj offering :namespace "offering") offering-props)
          offering-type (offering-version->type (:offering/version offering))
          label (name-label (:offering/name offering))
          supports-unregister? (offering-supports-unregister? offering-type (-> offering :offering/version bn/number))]
      (-> offering
          (assoc :offering/address (string/lower-case offering-address))
          (update :offering/version bn/number)
          (assoc :offering/type offering-type)
          (assoc :offering/auction? (= offering-type :auction-offering))
          (assoc :offering/buy-now? (= offering-type :buy-now-offering))
          (update :offering/price (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/number))
          (update :offering/created-on (if parse-dates? evm-time->local-date-time bn/number))
          (update :offering/finalized-on (if parse-dates? evm-time->local-date-time bn/number))
          (update :offering/original-owner string/lower-case)
          (update :offering/new-owner #(when-not (d0x-shared-utils/zero-address? %) (string/lower-case %)))
          (assoc :offering/name-level (name-level (:offering/name offering)))
          (assoc :offering/top-level-name? (top-level-name? (:offering/name offering)))
          (assoc :offering/label label)
          (assoc :offering/label-length (count label))
          (assoc :offering/contains-number? (contains-number? (:offering/name offering)))
          (assoc :offering/contains-special-char? (contains-special-char? (:offering/name offering)))
          (assoc :offering/contains-non-ascii? (contains-non-ascii? (:offering/name offering)))
          (assoc :offering/normalized? (normalized? (:offering/name offering)))
          (assoc :offering/valid-name? (valid-ens-name? (:offering/name offering)))
          (assoc :offering/supports-unregister? supports-unregister?)
          (assoc :offering/unregistered? (unregistered? supports-unregister? offering))))))

(defn parse-offering-ui [offering-address offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  "TODO: Delete this and replace usages with parse-offering once migrating UI to cljs-web3-next."
  (when offering
    (let [offering (zipmap offering-props offering)
          offering-type (offering-version->type (:offering/version offering))
          label (name-label (:offering/name offering))
          supports-unregister? (offering-supports-unregister? offering-type (-> offering :offering/version bn/number))]
      (-> offering
          (assoc :offering/address offering-address)
          (update :offering/version bn/number)
          (assoc :offering/type offering-type)
          (assoc :offering/auction? (= offering-type :auction-offering))
          (assoc :offering/buy-now? (= offering-type :buy-now-offering))
          (update :offering/price (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/number))
          (update :offering/created-on (if parse-dates? evm-time->local-date-time bn/number))
          (update :offering/finalized-on (if parse-dates? evm-time->local-date-time bn/number))
          (update :offering/new-owner #(when-not (d0x-shared-utils/zero-address? %) %))
          (assoc :offering/name-level (name-level (:offering/name offering)))
          (assoc :offering/top-level-name? (top-level-name? (:offering/name offering)))
          (assoc :offering/label label)
          (assoc :offering/label-length (count label))
          (assoc :offering/contains-number? (contains-number? (:offering/name offering)))
          (assoc :offering/contains-special-char? (contains-special-char? (:offering/name offering)))
          (assoc :offering/contains-non-ascii? (contains-non-ascii? (:offering/name offering)))
          (assoc :offering/normalized? (normalized? (:offering/name offering)))
          (assoc :offering/valid-name? (valid-ens-name? (:offering/name offering)))
          (assoc :offering/supports-unregister? supports-unregister?)
          (assoc :offering/unregistered? (unregistered? supports-unregister? offering))))))

(def auction-offering-props [:auction-offering/end-time :auction-offering/extension-duration
                             :auction-offering/bid-count :auction-offering/min-bid-increase
                             :auction-offering/winning-bidder])

(defn parse-auction-offering [auction-offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when auction-offering
    (-> (select-keys (jsobj->clj auction-offering :namespace "auction-offering") auction-offering-props)
        (update :auction-offering/end-time (if parse-dates? evm-time->local-date-time bn/number))
        (update :auction-offering/winning-bidder #(when-not (zero-address? %) (string/lower-case %)))
        (update :auction-offering/extension-duration bn/number)
        (update :auction-offering/min-bid-increase (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/number))
        (update :auction-offering/bid-count bn/number))))

(defn parse-auction-offering-ui [auction-offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  "TODO: Delete this and replace usages with parse-auction-offering once migrating UI to cljs-web3-next."
  (when auction-offering
    (-> (zipmap auction-offering-props auction-offering)
        (update :auction-offering/end-time (if parse-dates? evm-time->local-date-time bn/number))
        (update :auction-offering/winning-bidder #(when-not (zero-address? %) %))
        (update :auction-offering/extension-duration bn/number)
        (update :auction-offering/min-bid-increase (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/number))
        (update :auction-offering/bid-count bn/number))))

(def offering-request-props [:offering-request/name :offering-request/requesters-count :offering-request/latest-round])

(defn parse-offering-request [node offering-request]
  "TODO: Delete this and replace usages with parse-offering-request once migrating UI to cljs-web3-next."
  (when offering-request
    (-> (select-keys (jsobj->clj offering-request :namespace "offering-request") offering-request-props)
        (update :offering-request/requesters-count bn/number)
        (update :offering-request/latest-round bn/number)
        (assoc :offering-request/node node))))

(defn parse-offering-request-ui [node offering-request]
  (when offering-request
    (-> (zipmap offering-request-props offering-request)
        (update :offering-request/requesters-count bn/number)
        (update :offering-request/latest-round bn/number)
        (assoc :offering-request/node node))))

(def registrar-registration-props [:eth-registrar.registration/available
                                   :eth-registrar.registration/expiration-date
                                   :eth-registrar.registration/owner])

(defn calculate-min-bid
  ([price min-bid-increase bid-count]
   (calculate-min-bid price min-bid-increase bid-count 0))
  ([price min-bid-increase bid-count pending-returns]
   (let [min-bid-increase (if (pos? bid-count) min-bid-increase 0)]
     (bn/number (bn/- (bn/+ (web3/to-big-number price) min-bid-increase) pending-returns)))))
