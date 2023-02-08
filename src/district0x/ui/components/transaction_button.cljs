(ns district0x.ui.components.transaction-button
  (:require
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn transaction-button [props & children]
  (let [pending? (:pending? props)
        pending-text (get props :pending-text "Sending...")
        raised-button? (:raised-button? props)]
    (into [ui/Button
           (r/merge-props
             (dissoc props :raised-button? :pending-text :pending?)
             (merge
               (when-not @(subscribe [:district0x/can-make-transaction?])
                 {:disabled true})
               (when pending?
                 {:disabled true})))]
          (if pending? [pending-text] children))))


