(ns name-bazaar.ui.components.keyword-position-select
  (:require
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn keyword-position-select [props]
  [ui/Select
   (r/merge-props
     {:select-on-blur false
      :options [{:value :any :text "Contains"}
                {:value :start :text "Starts with"}
                {:value :end :text "Ends with"}]}
     props)])
