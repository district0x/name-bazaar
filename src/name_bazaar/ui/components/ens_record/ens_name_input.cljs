(ns name-bazaar.ui.components.ens-record.ens-name-input
  (:require
    [district0x.ui.components.input :refer [input]]
    [name-bazaar.ui.constants :as constants]
    [reagent.core :as r]
    [soda-ash.core :as ui]))


(defn ens-name-input [props]
  [input
   (r/merge-props
     {:action (r/as-element [ui/Label constants/registrar-root])}
     props)])
