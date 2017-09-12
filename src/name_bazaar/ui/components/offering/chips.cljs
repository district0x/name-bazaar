(ns name-bazaar.ui.components.offering.chips
  (:require
    [name-bazaar.ui.components.small-chip :refer [small-chip]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn offering-active-chip [props]
  [small-chip
   (r/merge-props
     {:background-color (styles/offering-status-chip-color :offering.status/active)}
     props)
   "active"])

(defn offering-sold-chip [props]
  [small-chip
   (r/merge-props
     {:background-color (styles/offering-status-chip-color :offering.status/finalized)}
     props)
   "sold"])

(defn offering-bought-chip [props]
  [small-chip
   (r/merge-props
     {:background-color (styles/offering-status-chip-color :offering.status/finalized)}
     props)
   "bought"])

(defn offering-auction-winning-chip [{:keys [:won?] :as props}]
  [small-chip
   (r/merge-props
     {:background-color (styles/offering-status-chip-color :offering.status/active)}
     (dissoc props :won?))
   (if won?
     "won"
     "winning")])

(defn offering-auction-pending-returns-chip [props]
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn []
      [small-chip
       (r/merge-props
         {:background-color (styles/offering-status-chip-color :offering.status/missing-ownership)}
         props)
       (if @xs?
         "pend. r."
         "pending ret.")])))




