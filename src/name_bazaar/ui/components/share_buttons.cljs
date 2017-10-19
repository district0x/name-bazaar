(ns name-bazaar.ui.components.share-buttons
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.string :as cstr]
            [name-bazaar.ui.utils :refer [path-for]]))

(defn share-buttons []
  (let [root-url (subscribe [:root-url])
        my-address (subscribe [:district0x/active-address])
        title "My ENS Offerings"
        page-url (reaction (cstr/replace
                            (str
                             @root-url
                             "/"
                             (path-for :route.user/offerings {:user/address (if @my-address
                                                                              (str @my-address)
                                                                              "")}))
                            "#"
                            "%23"))]
    (fn []
      (when @my-address
        [:div.share-buttons
         [:div.title "Share On:"]
         [:a {:target "_blank"
              :href (str "https://www.facebook.com/sharer/sharer.php?u=" @page-url "&title=" title)}
          [:i.icon.fb]]
         [:a {:target "_blank"
              :href (str "https://twitter.com/home?status=" title "+" @page-url)}
          [:i.icon.twitter]]]))))

