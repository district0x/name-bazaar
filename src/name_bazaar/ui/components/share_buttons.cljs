(ns name-bazaar.ui.components.share-buttons
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [reagent.ratom :refer-macros [reaction]]))

(defn share-buttons [{:keys [url title]}]
  [:div.share-buttons
   [:div.title "Share On:"]
   [:a {:target "_blank"
        :href (str "https://www.facebook.com/sharer/sharer.php?u=" url "&title=" title)}
    [:i.icon.fb]]
   [:a {:target "_blank"
        :href (str "https://twitter.com/home?status=" title "+" url)}
    [:i.icon.twitter]]])

