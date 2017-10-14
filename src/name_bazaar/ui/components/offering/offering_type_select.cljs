(ns name-bazaar.ui.components.offering.offering-type-select
  (:require
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn offering-type-select [props]
  [ui/Select
   (r/merge-props
     {:select-on-blur false
      :options [{:value :buy-now-offering :text "Buy Now"}
                {:value :auction-offering :text "Auction"}]}
     props)])
