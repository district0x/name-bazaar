(ns name-bazaar.ui.pages.offerings-search-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route.offerings/search []
  (fn []
    [:div "Search Offerings Page"]))
