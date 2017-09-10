(ns name-bazaar.ui.pages.ens-record-detail-page
  (:require
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [medley.core :as medley]
    [name-bazaar.ui.components.ens-name-details :refer [ens-name-details]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.search-fields.offerings-order-by-select-field :refer [offerings-order-by-select-field]]
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))


(defn ens-record-offerings-order-by-select-field []
  (let [search-results (subscribe [:offerings/ens-record-offerings])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [offerings-order-by-select-field
         {:order-by-column (first (:order-by-columns params))
          :order-by-dir (first (:order-by-dirs params))
          :options [:offering.order-by/newest
                    :offering.order-by/most-expensive
                    :offering.order-by/cheapest]
          :on-change (fn [order-by-column order-by-dir]
                       (dispatch [:offerings.ens-record-offerings/set-params-and-search
                                  {:order-by-columns [order-by-column]
                                   :order-by-dirs [order-by-dir]}
                                  {:clear-existing-items? true}]))}]))))

(defn ens-record-offerings []
  (let [route-params (subscribe [:district0x/route-params])
        search-results (subscribe [:offerings/ens-record-offerings])]
    (fn []
      (let [{:keys [:ens.record/name]} @route-params
            {:keys [:items :loading? :params :total-count]} @search-results]
        [paper
         {:style (merge styles/search-results-paper
                        {:min-height 200})}
         [:h1
          {:style styles/search-results-paper-headline}
          name " Offerings"]
         [row
          {:end "xs"
           :style styles/margin-bottom-gutter}
          [ens-record-offerings-order-by-select-field]]
         [search-results-infinite-list
          {:total-count total-count
           :offset (:offset params)
           :loading? loading?
           :no-items-text "No offerings were created for this name yet"
           :on-next-load (fn [offset limit]
                           (dispatch [:offerings.ens-record-offerings/set-params-and-search {:offset offset :limit limit}]))}
          (doall
            (for [[i offering] (medley/indexed items)]
              [offering-list-item
               {:key i
                :offering offering
                :header-props {:show-created-on? true
                               :show-sold-for? true
                               :show-active? true}}]))]]))))

(defmethod page :route.ens-record/detail []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      (let [{:keys [:ens.record/name]} @route-params]
        [side-nav-menu-center-layout
         [paper
          [:h1
           {:style styles/page-headline}
           name]
          [ens-name-details
           {:ens.record/name name
            :style {:padding 0}}]]
         [ens-record-offerings]]))))