(ns name-bazaar.ui.pages.user-bids-page
  (:require [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]))

(defmethod page :route.user/my-bids []
  [:div "My Bids"])

(defmethod page :route.user/bids []
  [:div "User Bids Page"])
