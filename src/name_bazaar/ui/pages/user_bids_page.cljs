(ns name-bazaar.ui.pages.user-bids-page
  (:require
    [cljs-web3.core :as web3]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.utils :refer [truncate]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.offerings-order-by-select :refer [offerings-order-by-select]]
    [name-bazaar.ui.utils :refer [user-name]]
    [re-frame.core :refer [subscribe dispatch]]
    [soda-ash.core :as ui]))

(defn user-bids-order-by-select []
  (let [search-results (subscribe [:offerings/user-bids])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [offerings-order-by-select
         {:order-by (:order-by params)
          :order-by-dir (:order-by-dir params)
          :fluid true
          :options [:offering.order-by/newest
                    :offering.order-by/most-active
                    :offering.order-by/most-expensive
                    :offering.order-by/cheapest
                    :offering.order-by/ending-soon]
          :on-change (fn [e data]
                       (let [[order-by-column order-by-dir] (aget data "value")]
                         (dispatch [:offerings.user-bids/set-params-and-search
                                    {:order-by order-by-column
                                     :order-by-dir order-by-dir}])))}]))))

(defn user-bids-search-checkboxes []
  (let [search-results (subscribe [:offerings/user-bids])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [:div
         [:div.grid.checkbox-filtering-options
          [ui/Checkbox
           {:label "Winning"
            :checked (boolean (:winning? params))
            :on-change #(dispatch [:offerings.user-bids/set-params-and-search
                                   {:winning? (aget %2 "checked")}])}]
          [ui/Checkbox
           {:label "Outbid"
            :checked (boolean (:outbid? params))
            :on-change #(dispatch [:offerings.user-bids/set-params-and-search
                                   {:outbid? (aget %2 "checked")}])}]
          [ui/Checkbox
           {:label "Finished auctions"
            :checked (not (:min-end-time-now? params))
            :on-change #(dispatch [:offerings.user-bids/set-params-and-search
                                   {:min-end-time-now? (not (aget %2 "checked"))}])}]]]))))

(defn user-bids []
  (let [search-results (subscribe [:offerings/user-bids])
        route-params (subscribe [:district0x/route-params])]
    (fn [{:keys [:title :no-items-text]}]
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [app-layout {:meta {:title (str "NameBazaar - " title)
                            :description (if-let [address (:user/address @route-params)]
                                           (str "See all bids for ENS name offerings of " address))}}
         [ui/Segment
          [:div
           [:div.grid.bids
            [:div.header [:h1.ui title]]
            [:div.search-checkboxes [user-bids-search-checkboxes]]
            [:div.order [user-bids-order-by-select]]]]
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
  (let [route-params (subscribe [:resolved-route-params])]
    (fn []
      [user-bids
       {:title (str (user-name (:user/address @route-params)) " Bids")
        :no-items-text "This user currently doesn't have bids in any active auction"}])))
