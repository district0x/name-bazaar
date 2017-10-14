(ns name-bazaar.ui.pages.user-offerings-page
  (:require
    [district0x.ui.components.misc :as misc :refer [page]]
    [district0x.ui.utils :refer [truncate]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.offerings-order-by-select :refer [offerings-order-by-select]]
    [name-bazaar.ui.components.share-buttons :refer [share-buttons]]
    [re-frame.core :refer [subscribe dispatch]]
    [soda-ash.core :as ui]))

(defn user-offerings-order-by-select []
  (let [search-results (subscribe [:offerings/user-offerings])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [offerings-order-by-select
         {:order-by-column (first (:order-by-columns params))
          :order-by-dir (first (:order-by-dirs params))
          :fluid true
          :options [:offering.order-by/newest
                    :offering.order-by/most-active
                    :offering.order-by/most-expensive
                    :offering.order-by/cheapest
                    :offering.order-by/ending-soon]
          :on-change (fn [e data]
                       (let [[order-by-column order-by-dir] (aget data "value")]
                         (dispatch [:offerings.user-offerings/set-params-and-search
                                    {:order-by-columns [order-by-column]
                                     :order-by-dirs [order-by-dir]}])))}]))))

(defn user-offerings-search-params []
  (let [search-results (subscribe [:offerings/user-offerings])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [ui/Grid
         {:class :checkbox-filtering-options
          :padded :vertically}
         [ui/GridColumn
          {:width 16}
          [ui/Checkbox
           {:label "Open"
            :checked (boolean (:open? params))
            :on-change #(dispatch [:offerings.user-offerings/set-params-and-search {:open? (aget %2 "checked")}])}]]
         [ui/GridColumn
          {:width 16}
          [ui/Checkbox
           {:label "Completed"
            :checked (boolean (:finalized? params))
            :on-change #(dispatch [:offerings.user-offerings/set-params-and-search {:finalized? (aget %2 "checked")}])}]]]))))

(defn user-offerings []
  (let [search-results (subscribe [:offerings/user-offerings])]
    (fn [{:keys [:title :no-items-text]}]
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [app-layout
         [ui/Segment
          [ui/Grid
           {:padded true
            :class "no-inner-horizontal-padding mobile-inner-vertical-padding"}
           [ui/GridRow
            {:class :join-upper}
            [ui/GridColumn
             {:computer 8
              :tablet 8
              :mobile 16}
             [:h1.ui.header title]]
            [ui/GridColumn
             {:computer 6
              :tablet 8
              :mobile 16
              :floated "right"}
             [share-buttons]]]
           [ui/GridRow
            {:vertical-align :bottom}
            [ui/GridColumn
             {:computer 8
              :tablet 8
              :mobile 16}
             [user-offerings-search-params]]
            [ui/GridColumn
             {:computer 6
              :tablet 8
              :mobile 16
              :floated "right"}
             [user-offerings-order-by-select]]]]
          [offering-infinite-list
           {:class "primary"
            :total-count total-count
            :offset (:offset params)
            :loading? loading?
            :no-items-text no-items-text
            :on-next-load (fn [offset limit]
                            (dispatch [:offerings.user-offerings/set-params-and-search
                                       {:offset offset :limit limit} {:append? true}]))}
           (doall
             (for [[i offering] (medley/indexed items)]
               [offering-list-item
                {:key (inc i)
                 :offering offering
                 :header-props {:show-sold? true
                                :show-missing-ownership? true}}]))]]]))))

(defmethod page :route.user/my-offerings []
  [user-offerings
   {:title "My Offerings"
    :no-items-text "You haven't created any offerings yet"}])

(defmethod page :route.user/offerings []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      [user-offerings
       {:title (str (truncate (:user/address @route-params) 10) " Offerings")
        :no-items-text "This user hasn't created any offerings yet"}])))
