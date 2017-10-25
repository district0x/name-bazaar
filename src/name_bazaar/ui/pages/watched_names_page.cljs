(ns name-bazaar.ui.pages.watched-names-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.ens-record.ens-name-input :refer [ens-name-input]]
    [name-bazaar.ui.components.infinite-list :refer [infinite-list expandable-list-item]]
    [name-bazaar.ui.components.offering-request.list-item :refer [offering-request-list-item]]
    [name-bazaar.ui.components.offering.list-header :refer [offering-list-header]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [valid-ens-name? normalize]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn- add-to-watched-names! [new-name-atom]
  (when (seq @new-name-atom)
    (dispatch [:watched-names/add (str @new-name-atom constants/registrar-root)])
    (reset! new-name-atom "")))

(defn add-watched-name-form []
  (let [new-name (r/atom "")]
    (fn []
      [:div.add-watched-name-form
       [ens-name-input
        {:label "Enter Name"
         :value @new-name
         :on-key-press (fn [e]
                         (when (= (aget e "key") "Enter")
                           (add-to-watched-names! new-name)))
         :on-change (fn [e data]
                      (let [value (aget data "value")]
                        (when (valid-ens-name? value)
                          (reset! new-name (normalize value)))))}]
       [:i.icon.plus-circle
        {:on-click #(add-to-watched-names! new-name)}]])))

(defn watch-item-placeholder []
  (let [mobile? (subscribe [:district0x.screen-size/mobile?])]
    (fn [{:keys [:watched-name] :as props}]
      (let [{:keys [:ens.record/name]} watched-name]
        [expandable-list-item
         {:disable-expand? true
          :collapsed-height (constants/infinite-list-collapsed-item-height @mobile?)}
         [:div.ui.grid.padded.search-results-list-item
          {:class (when @mobile? "mobile")}
          [ui/GridRow
           {:class "search-results-list-item-header opacity-1"
            :vertical-align :middle}
           [ui/GridColumn
            {:width 16}
            name]]]]))))

(defn watched-names-infinite-list []
  (let [mobile? (subscribe [:district0x.screen-size/mobile?])
        watched-items (subscribe [:watched-names/watched-items])]
    (fn []
      [:div.infinite-list-container
       [offering-list-header]
       [infinite-list
        {:collapsed-item-height (constants/infinite-list-collapsed-item-height @mobile?)
         :total-count (count @watched-items)
         :no-items-element (r/as-element [:div.no-items-text "You are not watching any names"])}
        (doall
          (for [{:keys [:ens.record/node] :as watched-item} @watched-items]
            (cond
              (:offering/address watched-item)
              [offering-list-item
               {:key node
                :offering watched-item}]

              (:offering-request/requesters-count watched-item)
              [offering-request-list-item
               {:key node
                :offering-request watched-item}]

              :else
              [watch-item-placeholder
               {:key node
                :watched-name watched-item}])))]])))

(defmethod page :route/watched-names []
  [app-layout
   [ui/Segment
    {:class "watched-names"}
    [:h1.ui.header.padded "Watched Names"]
    [ui/Grid
     {:class "layout-grid"}
     [ui/GridRow
      [ui/GridColumn
       {:computer 8
        :tablet 12
        :mobile 16}
       [add-watched-name-form]]]
     [ui/GridRow
      [ui/GridColumn
       {:class :join-upper
        :text-align :right}
       [:a.clear-all
        {:on-click #(dispatch [:watched-names/remove-all])}
        "Clear All"]]]
     [ui/GridRow
      [watched-names-infinite-list]]]]])
