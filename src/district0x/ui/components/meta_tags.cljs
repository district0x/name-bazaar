(ns district0x.ui.components.meta-tags
  (:require [reagent.core :as r]
            [cljsjs.react-meta-tags]))

(def meta-tags* (r/adapt-react-class (aget js/MetaTags "default")))
