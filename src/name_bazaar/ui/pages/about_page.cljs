(ns name-bazaar.ui.pages.about-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [soda-ash.core :as ui]))

(defmethod page :route/about []
  [app-layout
   [ui/Segment
    [:h1.ui.header.padded "About"]
    [:div.padded "Enter text here"]]])