(ns name-bazaar.ui.pages.home-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper]]
    [name-bazaar.ui.styles :as styles]))

(defn home-page []
  (fn []
    [center-layout
     [row
      [ui/auto-complete
       {:dataSource []
        :full-width true
        :floating-label-text "Search"}]]]))
