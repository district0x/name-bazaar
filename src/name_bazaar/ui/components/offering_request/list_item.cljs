(ns name-bazaar.ui.components.offering-request.list-item
  (:require
    [clojure.string :as string]
    [district0x.ui.utils :refer [pluralize]]
    [name-bazaar.ui.components.ens-name-details :refer [ens-name-details]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.loading-placeholders :refer [list-item-placeholder]]
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn offering-request-list-item-header []
  (let [visible? (r/atom false)
        mobile? (subscribe [:district0x.window.size/mobile?])]
    (fn [{:keys [:offering-request] :as props}]
      (let [{:keys [:offering-request/node :offering-request/name :offering-request/requesters-count]} offering-request]
        [:div.ui.grid.padded.search-results-list-item.offering-request
         (r/merge-props
           {:class (when @mobile? "mobile")}
           (dissoc props :offering-request))
         (if-not node
           [list-item-placeholder]
           [ui/GridRow
            {:class (str "search-results-list-item-header "
                         (when @visible? "opacity-1"))
             :ref #(reset! visible? true)
             :vertical-align :middle}
            [ui/GridColumn
             {:width 10}
             name]
            [ui/GridColumn
             {:width 6
              :text-align :right}
             requesters-count (pluralize " request" requesters-count)]])]))))

(defn offering-request-list-item []
  (let [mobile? (subscribe [:district0x.window.size/mobile?])]
    (fn [{:keys [:offering-request :expanded? :on-expand :key]}]
      (let [{:keys [:offering-request/node :offering-request/name]} offering-request]
        [expandable-list-item
         {:index key
          :on-expand #(dispatch [:offering-requests.list-item/expanded offering-request])
          :collapsed-height (constants/infinite-list-collapsed-item-height @mobile?)
          :disable-expand? (not node)}
         [offering-request-list-item-header
          {:offering-request offering-request}]
         [ens-name-details
          {:show-name-detail-link? true
           :ens.record/name name}]]))))
