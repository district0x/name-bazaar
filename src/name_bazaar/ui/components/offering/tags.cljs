(ns name-bazaar.ui.components.offering.tags
  (:require
    [name-bazaar.ui.utils :refer [offering-status->text offering-type->text offering-type->icon]]
    [re-frame.core :refer [subscribe]]
    [reagent.core :as r]))

(def offering-status->icon
  {:offering.status/emergency "exclamation-circle"
   :offering.status/active "pulse"
   :offering.status/finalized "check-circle"
   :offering.status/missing-ownership "user-question"
   :offering.status/auction-ended "hammer-down"})

(defn offering-status-tag [{:keys [:offering/status]} text]
  [:div.offering-tag.offering-status
   {:class (name status)}
   [:i.icon {:class (offering-status->icon status)}]
   (or text (offering-status->text status))])

(defn offering-type-tag [{:keys [:offering/type]}]
  [:div.offering-tag.offering-type
   {:class type}
   [:i.icon {:class (offering-type->icon type)}]
   (offering-type->text type)])

(defn offering-sold-tag [props]
  [:div.offering-tag.sold
   props
   "Sold"])

(defn offering-auction-winning-tag [{:keys [:won?] :as props}]
  [:div.offering-tag.auction-winning
   (dissoc props :won?)
   (if won?
     "Won"
     "Winning")])

(defn offering-auction-pending-returns-tag [props]
  [:div.offering-tag.pending-returns
   props
   "Pending ret."])




