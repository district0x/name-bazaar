(ns name-bazaar.ui.pages.about-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route/about []
  [:div "About Page"])