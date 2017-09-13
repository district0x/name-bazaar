(ns name-bazaar.ui.pages.about-page
  (:require
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.styles :as styles]))

(defmethod page :route/about []
  [side-nav-menu-center-layout
   [paper
    [:h1
     {:style styles/page-headline}
     "About"]
    [:div "Enter text here"]]])