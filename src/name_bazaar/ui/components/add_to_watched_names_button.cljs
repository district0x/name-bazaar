(ns name-bazaar.ui.components.add-to-watched-names-button
  (:require
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]))

(defn add-to-watched-names-button [{:keys [:ens.record/name]}]
  [:div
   [:a
    {:style styles/text-decor-none
     :on-click #(dispatch [:watched-names/add name])}
    "Add to Watched Names"]])