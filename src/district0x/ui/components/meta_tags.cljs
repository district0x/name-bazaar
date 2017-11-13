(ns district0x.ui.components.meta-tags
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [react-helmet]))

(def meta-tags (r/adapt-react-class (aget react-helmet "default")))
