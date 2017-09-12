(ns name-bazaar.ui.pages.user-bids-page
  (:require
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.utils :refer [truncate]]
    [medley.core :as medley]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.search-fields.offerings-order-by-select-field :refer [offerings-order-by-select-field]]
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [cljs-react-material-ui.reagent :as ui]))

(defn user-bids-order-by-select-field []
  (let [xs? (subscribe [:district0x/window-xs-width?])
        search-results (subscribe [:offerings/user-bids])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [offerings-order-by-select-field
         {:order-by-column (first (:order-by-columns params))
          :order-by-dir (first (:order-by-dirs params))
          :full-width (not @xs?)
          :options [:offering.order-by/newest
                    :offering.order-by/most-active
                    :offering.order-by/most-expensive
                    :offering.order-by/cheapest
                    :offering.order-by/ending-soon]
          :on-change (fn [order-by-column order-by-dir]
                       (dispatch [:offerings.user-bids/set-params-and-search
                                  {:order-by-columns [order-by-column]
                                   :order-by-dirs [order-by-dir]}]))}]))))

(defn user-bids-search-params []
  (let [search-results (subscribe [:offerings/user-bids])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [:div
         [ui/checkbox
          {:label "Winning"
           :checked (boolean (:winning? params))
           :on-check #(dispatch [:offerings.user-bids/set-params-and-search {:winning? %2}])}]
         [ui/checkbox
          {:label "Outbid"
           :checked (boolean (:outbid? params))
           :on-check #(dispatch [:offerings.user-bids/set-params-and-search {:outbid? %2}])}]
         [ui/checkbox
          {:label "Include finished auctions"
           :checked (not (:min-end-time-now? params))
           :on-check #(dispatch [:offerings.user-bids/set-params-and-search {:min-end-time-now? (not %2)}])}]]))))

(defn user-bids []
  (let [search-results (subscribe [:offerings/user-bids])]
    (fn [{:keys [:title :no-items-text]}]
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [side-nav-menu-center-layout
         [paper
          {:style styles/search-results-paper}
          [:h1
           {:style styles/search-results-paper-headline}
           title]
          [row-with-cols
           {:between "xs"
            :middle "xs"
            :style (merge styles/margin-top-gutter
                          styles/margin-bottom-gutter)}
           [col
            {:xs 12 :sm 4
             :style styles/margin-left-gutter-less}
            [user-bids-search-params]]
           [col
            {:xs 12 :sm 4
             :style styles/margin-left-gutter-less}
            [user-bids-order-by-select-field]]]
          [search-results-infinite-list
           {:total-count total-count
            :offset (:offset params)
            :loading? loading?
            :no-items-text no-items-text
            :on-next-load (fn [offset limit]
                            (dispatch [:offerings.user-bids/set-params-and-search {:offset offset :limit limit}]))}
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
    :no-items-text "Currently you don't have bid in any active auction"}])

(defmethod page :route.user/bids []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      [user-bids
       {:title (str (truncate (:user/address @route-params) 10) " Bids")
        :no-items-text "This user currently doesn't have bids in any active auction"}])))
