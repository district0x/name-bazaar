(ns district0x.ui.components.meta-tags
  (:require [reagent.core :as r]
            ["react-meta-tags" :refer [MetaTags]]))

(def meta-tags (r/adapt-react-class MetaTags))
