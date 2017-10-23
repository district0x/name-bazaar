(ns name-bazaar.ui.pages.how-it-works-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [soda-ash.core :as ui]))

(defmethod page :route/how-it-works []
  [app-layout
   [ui/Segment
    [:h1.ui.header.padded "How it works"]
    [:div.padded ""]]])
