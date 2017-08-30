(ns district0x.ui.components.transaction-button
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn transaction-button [{:keys [:pending? :pending-label :raised-button?] :as props
                           :or {:pending-label "Sending..."}}]
  [(if raised-button? ui/raised-button ui/flat-button)
   (r/merge-props
     (dissoc props :raised-button? :pending-label :pending?)
     (merge
       (when-not @(subscribe [:district0x/can-make-transaction?])
         {:disabled true})
       (when pending?
         {:disabled true
          :label pending-label})))])

(defn raised-transaction-button [props]
  [transaction-button (assoc props :raised-button? true)])


