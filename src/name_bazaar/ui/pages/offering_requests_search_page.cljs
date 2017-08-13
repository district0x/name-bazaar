(ns name-bazaar.ui.pages.offering-requests-search-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route.offering-requests/search []
  [:div "Search Offering Requests Page"])
