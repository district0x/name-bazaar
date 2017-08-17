(ns name-bazaar.ui.pages.user-offerings-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route.user/my-offerings []
  [:div "My Offerings"])

(defmethod page :route.user/offerings []
  [:div "User Offerings Page"])
