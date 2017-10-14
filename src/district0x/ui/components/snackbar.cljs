(ns district0x.ui.components.snackbar
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn snackbar []
  (let [snackbar (subscribe [:district0x/snackbar])]
    (fn [{:keys [:action-button-props] :as props}]
      (let [{:keys [:open? :message :action-href]} @snackbar]
        [:div.snackbar
         (r/merge-props
           {:class (when open? "open")}
           (dissoc props :action-button-props))
         [:div.snackbar-message message]
         (when action-href
           [ui/Button
            (r/merge-props
              {:class "action-button"
               :as "a"
               :href action-href}
              (dissoc action-button-props :text))
            (or (:text action-button-props) "Show Me")])]))))
