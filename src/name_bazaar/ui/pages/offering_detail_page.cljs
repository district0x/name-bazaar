(ns name-bazaar.ui.pages.offering-detail-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route.offerings/detail []
  [:div "Offering detail page"])
