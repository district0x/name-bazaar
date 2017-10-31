(ns name-bazaar.ui.pages.user-purchases-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.utils :refer [truncate]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.offerings-order-by-select :refer [offerings-order-by-select]]
    [re-frame.core :refer [subscribe dispatch]]
    [cljs-web3.core :as web3]
    [soda-ash.core :as ui]))

(defn user-purchases-order-by-select []
  (let [search-results (subscribe [:offerings/user-purchases])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [offerings-order-by-select
         {:order-by-column (first (:order-by-columns params))
          :order-by-dir (first (:order-by-dirs params))
          :fluid true
          :options [:offering.order-by/finalized-newest
                    :offering.order-by/finalized-oldest
                    :offering.order-by/cheapest
                    :offering.order-by/most-expensive]
          :on-change (fn [e data]
                       (let [[order-by-column order-by-dir] (aget data "value")]
                         (dispatch [:offerings.user-purchases/set-params-and-search
                                    {:order-by-columns [order-by-column]
                                     :order-by-dirs [order-by-dir]}])))}]))))

(defn user-purchases []
  (let [search-results (subscribe [:offerings/user-purchases])]
    (fn [{:keys [:title :no-items-text]}]
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [app-layout
         [ui/Segment
          [ui/Grid
           {:padded true
            :class :no-inner-horizontal-padding}
           [ui/GridColumn
            {:width 16
             :class :join-upper}
            [:h1.ui.header title]]
           [ui/GridColumn
            {:computer 6
             :tablet 8
             :mobile 16
             :floated "right"}
            [user-purchases-order-by-select]]]
          [offering-infinite-list
           {:class "primary"
            :total-count total-count
            :offset (:offset params)
            :loading? loading?
            :no-items-text no-items-text
            :header-props {:show-time-ago? true
                           :show-sold-for? true}
            :on-next-load (fn [offset limit]
                            (dispatch [:offerings.user-purchases/set-params-and-search
                                       {:offset offset :limit limit} {:append? true}]))}
           (doall
             (for [[i offering] (medley/indexed items)]
               [offering-list-item
                {:key i
                 :offering offering
                 :header-props {:show-finalized-on? true}}]))]]]))))

(defmethod page :route.user/my-purchases []
  [user-purchases
   {:title "My Purchases"
    :no-items-text "You have not purchased any names yet"}])

(defmethod page :route.user/purchases []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      [user-purchases
       {:title (str ((if-not (web3/address? (:user/address @route-params))
                       identity
                       #(truncate % 10)) (:user/address @route-params)) " Purchases")
        :no-items-text "This user hasn't purchased any names yet"}])))
