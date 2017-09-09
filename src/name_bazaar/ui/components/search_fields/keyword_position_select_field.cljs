(ns name-bazaar.ui.components.search-fields.keyword-position-select-field
  (:require
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(defn keyword-position-select-field [props]
  [ui/select-field
   (r/merge-props
     {:full-width true
      :hint-text "Keyword Position"}
     props)
   (for [[val text] [[:contain "Contains"] [:start "Starts with"] [:end "Ends with"]]]
     [ui/menu-item
      {:key val
       :value val
       :primary-text text}])])
