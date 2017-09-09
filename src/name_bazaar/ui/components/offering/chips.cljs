(ns name-bazaar.ui.components.offering.chips
  (:require
    [name-bazaar.ui.components.small-chip :refer [small-chip]]
    [name-bazaar.ui.styles :as styles]
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




