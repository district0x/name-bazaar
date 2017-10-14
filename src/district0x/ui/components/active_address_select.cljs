(ns district0x.ui.components.active-address-select
  (:require
    [district0x.ui.utils :refer [truncate]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn active-address-select []
  (let [my-addresses (subscribe [:district0x/my-addresses])
        active-address (subscribe [:district0x/active-address])]
    (fn [{:keys [:select-field-props :single-address-props]}]
      (when (seq @my-addresses)
        (if (= 1 (count @my-addresses))
          [:div.active-address-select.single-address
           single-address-props
           (truncate @active-address (or (:single-address-truncate single-address-props) 20))]
          [ui/Select
           (r/merge-props
             {:select-on-blur false
              :class "active-address-select"
              :value @active-address
              :on-change (fn [e data]
                           (dispatch [:district0x/set-active-address (aget data "value")]))
              :fluid true
              :options (doall (for [address @my-addresses]
                                {:value address :text (truncate address 20)}))}
             select-field-props)])))))
