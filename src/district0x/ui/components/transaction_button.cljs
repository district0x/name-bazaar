(ns district0x.ui.components.transaction-button
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn transaction-button [{:keys [:pending? :pending-text :raised-button?] :as props
                           :or {:pending-text "Sending..."}} & children]
  (into [ui/Button
         (r/merge-props
           (dissoc props :raised-button? :pending-text :pending?)
           (merge
             (when-not @(subscribe [:district0x/can-make-transaction?])
               {:disabled true})
             (when pending?
               {:disabled true})))]
        (if pending?
          [pending-text]
          children)))


