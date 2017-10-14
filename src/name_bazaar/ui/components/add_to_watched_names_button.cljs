(ns name-bazaar.ui.components.add-to-watched-names-button
  (:require
    [name-bazaar.ui.utils :refer [namehash]]
    [re-frame.core :refer [subscribe dispatch]]))

(defn add-to-watched-names-button [{:keys [:ens.record/name]}]
  (let [node (namehash name)
        watched? @(subscribe [:watched-names.node/watched? node])
        on-click-event (if watched? :watched-names/remove :watched-names/add)]
    [:div
     [:a.no-decor
      {:on-click (fn []
                   (if watched?
                     (dispatch [:watched-names/remove node])
                     (dispatch [:watched-names/add name])))}
      (if watched?
        "Remove from"
        "Add to")
      " Watched Names"]]))