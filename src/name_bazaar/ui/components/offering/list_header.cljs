(ns name-bazaar.ui.components.offering.list-header
  (:require
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [subscribe dispatch]]
    [soda-ash.core :as ui]))

(defn offering-list-header []
  (let [mobile? (subscribe [:district0x.screen-size/mobile?])]
    (fn [{:keys [:show-time-ago?] :as props}]
      [:div.ui.grid.padded.search-results-list-item.list-header.opacity-1
       (dissoc props :show-time-ago?)
       [ui/GridRow
        {:style {:height (constants/infinite-list-collapsed-item-height @mobile?)}
         :vertical-align :middle}
        [ui/GridColumn
         {:width 6}]
        [ui/GridColumn
         {:width 3
          :text-align :center}
         [:div "Bids"]]
        [ui/GridColumn
         {:width 3
          :text-align :center}
         [:div (if show-time-ago?
                 "Time Ago"
                 "Time Left")]]]])))
