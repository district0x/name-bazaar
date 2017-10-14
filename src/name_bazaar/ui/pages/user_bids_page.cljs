(ns name-bazaar.ui.pages.user-bids-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.utils :refer [truncate]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.offerings-order-by-select :refer [offerings-order-by-select]]
    [re-frame.core :refer [subscribe dispatch]]
    [soda-ash.core :as ui]))

(defn user-bids-order-by-select []
  (let [search-results (subscribe [:offerings/user-bids])]
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
                         (dispatch [:offerings.user-bids/set-params-and-search
                                    {:order-by-columns [order-by-column]
                                     :order-by-dirs [order-by-dir]}])))}]))))

(defn user-bids-search-checkboxes []
  (let [search-results (subscribe [:offerings/user-bids])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [ui/Grid
         {:class :checkbox-filtering-options
          :padded :vertically}
         [ui/GridColumn
          {:width 16}
          [ui/Checkbox
           {:label "Winning"
            :checked (boolean (:winning? params))
            :on-change #(dispatch [:offerings.user-bids/set-params-and-search {:winning? (aget %2 "checked")}])}]]
         [ui/GridColumn
          {:width 16}
          [ui/Checkbox
           {:label "Outbid"
            :checked (boolean (:outbid? params))
            :on-change #(dispatch [:offerings.user-bids/set-params-and-search {:outbid? (aget %2 "checked")}])}]]
         [ui/GridColumn
          {:width 16}
          [ui/Checkbox
           {:label "Finished auctions"
            :checked (not (:min-end-time-now? params))
            :on-change #(dispatch [:offerings.user-bids/set-params-and-search {:min-end-time-now? (not (aget %2 "checked"))}])}]]]))))

(defn user-bids []
  (let [search-results (subscribe [:offerings/user-bids])]
    (fn [{:keys [:title :no-items-text]}]
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [app-layout
         [ui/Segment
          [ui/Grid
           {:padded true
            :class "no-inner-horizontal-padding mobile-inner-vertical-padding"}
           [ui/GridColumn
            {:width 16
             :class :join-upper}
            [:h1.ui.header title]]
           [ui/GridColumn
            {:computer 8
             :tablet 16
             :mobile 16}
            [user-bids-search-checkboxes]]
           [ui/GridColumn
            {:computer 6
             :tablet 8
             :mobile 16
             :floated "right"
             :vertical-align :bottom}
            [user-bids-order-by-select]]]
          [offering-infinite-list
           {:class "primary"
            :total-count total-count
            :offset (:offset params)
            :loading? loading?
            :no-items-text no-items-text
            :on-next-load (fn [offset limit]
                            (dispatch [:offerings.user-bids/set-params-and-search
                                       {:offset offset :limit limit} {:append? true}]))}
           (doall
             (for [[i offering] (medley/indexed items)]
               [offering-list-item
                {:key i
                 :offering offering
                 :header-props {:show-auction-winning? true
                                :show-auction-pending-returns? true}}]))]]]))))

(defmethod page :route.user/my-bids []
  [user-bids
   {:title "My Bids"
    :no-items-text "You don't have bid in any active auction currently"}])

(defmethod page :route.user/bids []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      [user-bids
       {:title (str (truncate (:user/address @route-params) 10) " Bids")
        :no-items-text "This user currently doesn't have bids in any active auction"}])))
