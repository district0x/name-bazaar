(ns name-bazaar.ui.components.search-fields.keyword-text-field
  (:require
    [district0x.ui.components.text-field :refer [text-field]]
    [reagent.core :as r]))

(defn keyword-text-field [props]
  [text-field
   (r/merge-props
     {:floating-label-text "Keyword"
      :full-width true}
     props)])