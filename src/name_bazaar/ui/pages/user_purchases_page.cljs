(ns name-bazaar.ui.pages.user-purchases-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route.user/my-purchases []
  [:div "My Purchases"])

(defmethod page :route.user/purchases []
  [:div "User Purchases Page"])
