(ns name-bazaar.ui.components.search-fields.keyword-position-select-field
  (:require
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(defn keyword-position-select-field [props]
  [ui/select-field
   (r/merge-props
     {:full-width true
      :floating-label-text "Keyword Position"}
     props)
   (for [[val text] [[:any "Any"] [:start "Start"] [:end "End"]]]
     [ui/menu-item
      {:key val
       :value val
       :primary-text text}])])
