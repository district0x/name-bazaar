(ns name-bazaar.ui.pages.offerings-search-page
  (:require
    [cemerick.url :as url]
    [district0x.shared.utils :refer [non-neg-ether-value?]]
    [district0x.ui.components.input :refer [input]]
    [district0x.ui.components.misc :refer [page]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.keyword-position-select :refer [keyword-position-select]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.offering-type-select :refer [offering-type-select]]
    [name-bazaar.ui.components.offering.offerings-order-by-select :refer [offerings-order-by-select]]
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.utils :refer [offerings-sold-query-params]]
    [print.foo :refer [look]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]
    ))

(defn offerings-keyword-text-field []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [input
       {:label "Keyword"
        :fluid true
        :value (:name @search-params)
        :on-change #(dispatch [:district0x.location/add-to-query {:name (aget %2 "value")}])}])))

(defn offerings-keyword-position-select []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [keyword-position-select
       {:fluid true
        :value (:name-position @search-params)
        :on-change #(dispatch [:district0x.location/add-to-query {:name-position (aget %2 "value")}])}])))

(defn saved-searches-select []
  (let [saved-searches (subscribe [:offerings/saved-searches])
        query-string (subscribe [:district0x/query-string])]
    [ui/Select
     {:select-on-blur false
      :placeholder "Saved Searches"
      :fluid true
      :disabled (empty? @saved-searches)
      :value (when (get @saved-searches @query-string) @query-string)
      :options (for [[value text] @saved-searches]
                 {:value value :text text})
      :on-change #(dispatch [:district0x.location/set-query (aget %2 "value")])}]))

(defn save-search-button []
  (let [open? (r/atom false)
        query-string (subscribe [:district0x/query-string])
        saved-search-active? (subscribe [:offerings.saved-search/active?])
        saved-search-name (r/atom "")]
    (fn [props]
      [ui/Modal
       (r/merge-props
         {:dimmer false
          :open @open?
          :on-open (fn []
                     (if @saved-search-active?
                       (dispatch [:offerings.saved-searches/remove @query-string])
                       (when-not (empty? @query-string)
                         (reset! open? true))))
          :on-close #(reset! open? false)
          :trigger (r/as-element [:i.icon.search-options-icon-button
                                  {:class (str
                                            (when (empty? @query-string) "disabled ")
                                            (if @saved-search-active? "bookmark-remove" "bookmark"))}])
          :size :small}
         (dissoc props :on-confirm :on-cancel))
       [ui/ModalHeader "Save Search"]
       [ui/ModalContent
        [:div
         [:div.grid.search
          [input
           {:label "Search Name"
            :fluid true
            :value @saved-search-name
            :on-change #(reset! saved-search-name (aget %2 "value"))}]
          [:div.description
           "Note: Search is saved only in your browser. It's not stored on a blockchain or a server"]]]]
       [ui/ModalActions
        [ui/Button
         {:secondary true
          :on-click #(reset! open? false)}
         "Cancel"]
        [ui/Button
         {:primary true
          :disabled (empty? @saved-search-name)
          :on-click (fn []
                      (dispatch [:offerings.saved-searches/add @query-string @saved-search-name])
                      (reset! open? false))}
         "Save"]]])))

(defn buy-now-offerings-checkbox []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [ui/Checkbox
       {:label "Buy Now Offerings"
        :checked (boolean (:buy-now? @search-params))
        :on-change #(dispatch [:district0x.location/add-to-query {:buy-now? (aget %2 "checked")}])}])))

(defn auction-offerings-checkbox []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [ui/Checkbox
       {:label "Auction Offerings"
        :checked (boolean (:auction? @search-params))
        :on-change #(dispatch [:district0x.location/add-to-query {:auction? (aget %2 "checked")}])}])))

(defn top-level-names-checkbox []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [ui/Checkbox
       {:label "Top Level Names"
        :checked (boolean (:top-level-names? @search-params))
        :on-change #(dispatch [:district0x.location/add-to-query {:top-level-names? (aget %2 "checked")}])}])))

(defn subnames-checkbox []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [ui/Checkbox
       {:label "Subnames"
        :checked (boolean (:sub-level-names? @search-params))
        :on-change #(dispatch [:district0x.location/add-to-query {:sub-level-names? (aget %2 "checked")}])}])))

(defn exclude-numbers-checkbox []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [ui/Checkbox
       {:label "Exclude Numbers"
        :checked (boolean (:exclude-numbers? @search-params))
        :on-change #(dispatch [:district0x.location/add-to-query {:exclude-numbers? (aget %2 "checked")}])}])))

(defn exclude-special-chars-checkbox []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      [ui/Checkbox
       {:label "Exclude Special Char."
        :checked (boolean (:exclude-special-chars? @search-params))
        :on-change #(dispatch [:district0x.location/add-to-query {:exclude-special-chars? (aget %2 "checked")}])}])))

(defn min-price-input []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      (let [{:keys [:min-price]} @search-params]
        [input
         {:label "Min. Price"
          :fluid true
          :value min-price
          :error (not (non-neg-ether-value? min-price {:allow-empty? true}))
          :on-change #(dispatch [:district0x.location/add-to-query {:min-price (aget %2 "value")}])}]))))

(defn max-price-input []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      (let [{:keys [:max-price]} @search-params]
        [input
         {:label "Max. Price"
          :fluid true
          :value max-price
          :error (not (non-neg-ether-value? max-price {:allow-empty? true}))
          :on-change #(dispatch [:district0x.location/add-to-query {:max-price (aget %2 "value")}])}]))))

(defn min-length-input []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      (let [{:keys [:min-length]} @search-params]
        [input
         {:label "Min. Length"
          :fluid true
          :value min-length
          :error (not (non-neg-ether-value? min-length {:allow-empty? true}))
          :on-change #(dispatch [:district0x.location/add-to-query {:min-length (aget %2 "value")}])}]))))

(defn max-length-input []
  (let [search-params (subscribe [:offerings.main-search/params])]
    (fn []
      (let [{:keys [:max-length]} @search-params]
        [input
         {:label "Max. Length"
          :fluid true
          :value max-length
          :error (not (non-neg-ether-value? max-length {:allow-empty? true}))
          :on-change #(dispatch [:district0x.location/add-to-query {:max-length (aget %2 "value")}])}]))))

(defn order-by-select-field []
  (let [search-params (subscribe [:offerings.main-search/params])
        sold-page? (subscribe [:offerings.main-search/sold-page?])]
    (fn []
      [offerings-order-by-select
       {:fluid true
        :order-by (:order-by @search-params)
        :order-by-dir (:order-by-dir @search-params)
        :options (concat
                   [:offering.order-by/newest
                    :offering.order-by/most-active
                    :offering.order-by/most-expensive
                    :offering.order-by/cheapest
                    :offering.order-by/ending-soon
                    :offering.order-by/most-relevant]
                   (when @sold-page?
                     [:offering.order-by/finalized-newest
                      :offering.order-by/finalized-oldest]))
        :on-change (fn [e data]
                     (let [[order-by-column order-by-dir] (aget data "value")]
                       (dispatch [:district0x.location/add-to-query
                                  {:order-by order-by-column
                                   :order-by-dir order-by-dir}])))}])))

(defn reset-filter-button []
  (let [sold-page? (subscribe [:offerings.main-search/sold-page?])]
    (fn [{:keys [:on-click] :as props}]
      [:i.icon.filter.search-options-icon-button
       (r/merge-props
         props
         {:on-click (fn []
                      (when (fn? on-click)
                        (on-click))
                      (dispatch [:district0x.location/set-query (if @sold-page?
                                                                  offerings-sold-query-params
                                                                  "")]))})])))

(defn search-params-panel []
  (let [open? (r/atom false)
        mobile? (subscribe [:district0x.window.size/mobile?])]
    (fn []
      [ui/Segment
       (concat
        [[:div.grid.search-params {:key :search-params}
          (concat
           [[:div.keyword {:key :keyword}
              [offerings-keyword-text-field]]]

           (when @mobile?
             [[:div.grid.offerings-search-options-section-mobile {:key :offerings-search-options-section-mobile}
               [:div.order-by-select
                [order-by-select-field]]
               [:div.reset-filter
                [reset-filter-button
                 {:on-click #(reset! open? false)}]]]])

           [[:div.offerings-mode-section {:key :offerings-mode-section}
             [:div.grid.keyword-position
              (when-not @mobile?
                [:div.position-selector
                 [offerings-keyword-position-select]])
              [:div.saved-searches-select
               [saved-searches-select]]
              [:div.saved-button
               [save-search-button]]]]]

           (if (or (not @mobile?) @open?)
             [[:div.grid.checkbox-filtering-options {:key :checkbox-filtering-options}
               [:div.checkbox.buy-now
                [buy-now-offerings-checkbox]]
               [:div.checkbox.subnames
                [subnames-checkbox]]
               [:div.checkbox.auction-offerings
                [auction-offerings-checkbox]]
               [:div.checkbox.exclude-numbers
                [exclude-numbers-checkbox]]
               [:div.checkbox.tld
                [top-level-names-checkbox]]
               [:div.checkbox.specialchars
                [exclude-special-chars-checkbox]]]
              [:div.grid.offerings-search-options-section {:key :offerings-search-options-section}
               [:div.min-price
                [min-price-input]]
               [:div.max-price
                [max-price-input]]
               [:div.min-length
                [min-length-input]]
               [:div.max-length
                [max-length-input]]
               (when-not @mobile?
                 [:div.order-by-select
                  [order-by-select-field]])
               (when-not @mobile?
                 [:div.reset-filter
                  [reset-filter-button]])]]
             [[:div.show-advanced-search-options
               {:on-click #(reset! open? true)}
               "Show Advanced Options ▾"]]))]])])))

(defn offerings-search-results []
  (let [search-results (subscribe [:offerings/main-search])
        sold-page? (subscribe [:offerings.main-search/sold-page?])
        mobile? (subscribe [:district0x.window.size/mobile?])]
    (fn []
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [ui/Segment
         [offering-infinite-list
          {:class "primary"
           :total-count total-count
           :offset (:offset params)
           :loading? loading?
           :no-items-text "No offerings found matching your search criteria"
           :header-props {:show-time-ago? @sold-page?
                          :show-sold-for? @sold-page?}
           :on-next-load (fn [offset limit]
                           (dispatch [:offerings.main-search/set-params-and-search
                                      {:offset offset :limit limit}
                                      {:append? true}]))}
          (doall
            (for [[i offering] (medley/indexed items)]
              ^{:key i}
              [offering-list-item
               {:key i
                :offering offering
                :header-props {:show-finalized-on? @sold-page?
                               :show-sold? (and @sold-page? @mobile?)}}]))]]))))

(defmethod page :route.offerings/search []
  (let [xs-sm? (subscribe [:district0x.window.size/max-tablet?])]
    (fn []
      [app-layout {:meta {:title "NameBazaar - Search ENS Offerings"}}
       [search-params-panel]
       [offerings-search-results]])))
