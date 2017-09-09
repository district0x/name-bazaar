(ns name-bazaar.ui.components.small-chip
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [name-bazaar.ui.styles :as styles]
    [reagent.core :as r]))

(defn small-chip [props & children]
  (into [ui/chip
         (r/merge-props
           {:style {:display :inline-block}
            :label-style styles/small-chip-label}
           props)]
        children))
