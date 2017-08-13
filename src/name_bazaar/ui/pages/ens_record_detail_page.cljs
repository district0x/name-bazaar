(ns name-bazaar.ui.pages.ens-record-detail-page
  (:require
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]
    [re-frame.core :refer [subscribe dispatch]]))

(defmethod page :route.ens-record/detail []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      [:div "ENS Record Detail " (:ens.record/name @route-params)])))