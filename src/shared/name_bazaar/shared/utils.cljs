(ns name-bazaar.shared.utils
  (:require
    [district0x.big-number :as bn]
    [district0x.utils :as u]))

(defn offering-version->type [offering-type]
  (if (>= (bn/->number offering-type) 100000)
    :english-auction-offering
    :instant-buy-offering))

(def offering-props [:offering/offering-registry :offering/ens :offering/node :offering/name :offering/original-owner
                     :offering/emergency-multisig :offering/version :offering/created-on :offering/new-owner
                     :offering/price])

(defn parse-offering [offering-address offering & [{:keys [:parse-dates?]}]]
  (when offering
    (-> (zipmap offering-props offering)
      (assoc :offering/address offering-address)
      (update :offering/version bn/->number)
      (assoc :offering/type (offering-version->type (:offering/version offering)))
      (update :offering/price bn/->number)
      (update :offering/created-on (if parse-dates? bn/->date-time bn/->number))
      (update :offering/new-owner #(when-not (u/zero-address? %))))))

(def english-auction-offering-props [:english-auction-offering/end-time :english-auction-offering/extension-duration
                                     :english-auction-offering/min-bid-increase :english-auction-offering/winning-bidder])

(defn parse-english-auction-offering [english-auction-offering & [{:keys [:parse-dates?]}]]
  (when english-auction-offering
    (-> (zipmap english-auction-offering-props english-auction-offering)
      (update :english-auction-offering/end-time (if parse-dates? bn/->date-time bn/->number))
      (update :english-auction-offering/extension-duration bn/->number)
      (update :english-auction-offering/min-bid-increase bn/->number))))

(defn parse-offering-requests-counts [nodes counts]
  (zipmap nodes (map #(hash-map :offering-request/requesters-count (bn/->number %)) counts)))

(def ens-record-props [:ens.record/owner :ens.record/resolver :ens.record/ttl])

(defn parse-ens-record [node ens-record & [{:keys [:parse-dates?]}]]
  (when ens-record
    (-> (zipmap ens-record-props ens-record)
      (update :ens.record/ttl bn/->number)
      (assoc :ens.record/node node))))


