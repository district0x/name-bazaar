(ns name-bazaar.ui.pages.offerings-search-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljsjs.react-infinite]
    [clojure.string :as string]
    [components.offering-list-item :refer [offering-list-item]]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.text-field :refer [text-field ether-field integer-field ether-field-with-currency]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code]]
    [medley.core :as medley]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.components.infinite-list :refer [infinite-list]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-layout]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn keyword-text-field []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [props]
      [text-field
       (r/merge-props
         {:floating-label-text "Keyword"
          :full-width true
          :value (:name @search-params)
          :on-change #(dispatch [:set-params-and-search-offerings-main-search {:name %2} {:add-to-query? true}])}
         props)])))

(defn keyword-position-select-field []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [props]
      [ui/select-field
       (r/merge-props
         {:full-width true
          :hint-text "Keyword Position"
          :value (:name-position @search-params)
          :on-change #(dispatch [:set-params-and-search-offerings-main-search {:name-position %3} {:add-to-query? true}])}
         props)
       (for [[val text] [[:contain "Contains"] [:start "Starts with"] [:end "Ends with"]]]
         [ui/menu-item
          {:key val
           :value val
           :primary-text text}])])))

(defn saved-searches-select-field []
  (let [saved-searches (subscribe [:saved-searches :offerings-search])
        query-string (subscribe [:district0x/query-string])]
    (fn [props]
      [ui/select-field
       (r/merge-props
         (merge
           {:style styles/saved-searches-select-field
            :hint-text "Saved Searches"
            :on-change #(dispatch [:district0x.location/set-query %3])}
           (when (get @saved-searches @query-string)
             {:value @query-string}))
         props)
       (for [[query text] @saved-searches]
         [ui/menu-item
          {:key query
           :value query
           :primary-text text}])])))

(defn save-search-dialog []
  (let [saved-search-name (r/atom "")]
    (fn [{:keys [:on-confirm :on-cancel] :as props}]
      [ui/dialog
       (r/merge-props
         {:title "Save Search"
          :actions [(r/as-element
                      [ui/flat-button
                       {:label "Cancel"
                        :secondary true
                        :on-click on-cancel}])
                    (r/as-element
                      [ui/flat-button
                       {:label "Save"
                        :primary true
                        :disabled (empty? @saved-search-name)
                        :keyboard-focused true
                        :on-click (fn []
                                    (on-confirm @saved-search-name))}])]}
         (dissoc props :on-confirm :on-cancel))
       [:div
        [text-field
         {:floating-label-text "Search Name"
          :value @saved-search-name
          :on-change #(reset! saved-search-name %2)}]]])))

(defn save-search-button []
  (let [dialog-open? (r/atom false)
        query-string (subscribe [:district0x/query-string])
        saved-search-active? (subscribe [:saved-search-active? :offerings-search])]
    (fn [props]
      [:div
       [save-search-dialog
        {:open @dialog-open?
         :on-cancel #(reset! dialog-open? false)
         :on-confirm (fn [saved-search-name]
                       (dispatch [:saved-searches/add :offerings-search @query-string saved-search-name])
                       (reset! dialog-open? false))}]
       (if @saved-search-active?
         [ui/icon-button
          (r/merge-props
            {:tooltip "Delete this saved search"
             :on-click #(dispatch [:saved-searches/remove :offerings-search @query-string])}
            props)
          (icons/bookmark-remove)]
         [ui/icon-button
          (r/merge-props
            {:tooltip "Save current search"
             :disabled (empty? @query-string)
             :on-click #(reset! dialog-open? true)}
            props)
          (icons/bookmark-outline)])])))

(defn offering-type-checkbox-group []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [{:keys [:buy-now-checkbox-props :auction-checkbox-props]}]
      [:div
       {:style styles/full-width}
       [ui/checkbox
        (r/merge-props
          {:label "Buy Now Offerings"
           :checked (boolean (:buy-now? @search-params))
           :on-check #(dispatch [:set-params-and-search-offerings-main-search {:buy-now? %2} {:add-to-query? true}])}
          buy-now-checkbox-props)]
       [ui/checkbox
        (r/merge-props
          {:label "Auction Offerings"
           :checked (boolean (:auction? @search-params))
           :on-check #(dispatch [:set-params-and-search-offerings-main-search {:auction? %2} {:add-to-query? true}])}
          auction-checkbox-props)]])))

(defn name-level-checkbox-group []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [{:keys [:top-level-checkbox-props :subname-checkbox-props]}]
      [:div
       {:style styles/full-width}
       [ui/checkbox
        (r/merge-props
          {:label "Top Level Names"
           :checked (boolean (:top-level-names? @search-params))
           :on-check #(dispatch [:set-params-and-search-offerings-main-search {:top-level-names? %2} {:add-to-query? true}])}
          top-level-checkbox-props)]
       [ui/checkbox
        (r/merge-props
          {:label "Subnames"
           :checked (boolean (:sub-level-names? @search-params))
           :on-check #(dispatch [:set-params-and-search-offerings-main-search {:sub-level-names? %2} {:add-to-query? true}])}
          subname-checkbox-props)]])))

(defn exclude-chars-checkbox-group []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [{:keys [:exclude-numbers-checkbox-props :exclude-spec-chars-checkbox-props]}]
      [:div
       {:style styles/full-width}
       [ui/checkbox
        (r/merge-props
          {:label "Exclude Numbers"
           :checked (boolean (:exclude-numbers? @search-params))
           :on-check #(dispatch [:set-params-and-search-offerings-main-search {:exclude-numbers? %2} {:add-to-query? true}])}
          exclude-numbers-checkbox-props)]
       [ui/checkbox
        (r/merge-props
          {:label "Exclude Special Char."
           :checked (boolean (:exclude-special-chars? @search-params))
           :on-check #(dispatch [:set-params-and-search-offerings-main-search {:exclude-special-chars? %2} {:add-to-query? true}])}
          exclude-spec-chars-checkbox-props)]])))

(defn price-text-fields []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [{:keys [:min-price-text-field-props :max-price-text-field-props]}]
      [row-with-cols
       [col
        {:xs 6}
        [ether-field
         (r/merge-props
           {:floating-label-text "Min. Price"
            :full-width true
            :allow-empty? true
            :value (:min-price @search-params)
            :on-change #(dispatch [:set-params-and-search-offerings-main-search {:min-price %2} {:add-to-query? true}])}
           min-price-text-field-props)]]
       [col
        {:xs 6}
        [ether-field
         (r/merge-props
           {:floating-label-text "Max. Price"
            :full-width true
            :allow-empty? true
            :value (:max-price @search-params)
            :on-change #(dispatch [:set-params-and-search-offerings-main-search {:max-price %2} {:add-to-query? true}])}
           max-price-text-field-props)]]])))

(defn length-text-fields []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [{:keys [:min-length-text-field-props :max-length-text-field-props]}]
      [row-with-cols
       [col
        {:xs 6}
        [integer-field
         (r/merge-props
           {:floating-label-text "Min. Length"
            :full-width true
            :allow-empty? true
            :value (:min-length @search-params)
            :on-change #(dispatch [:set-params-and-search-offerings-main-search {:min-length %2} {:add-to-query? true}])}
           min-length-text-field-props)]]
       [col
        {:xs 6}
        [integer-field
         (r/merge-props
           {:floating-label-text "Max. Length"
            :full-width true
            :allow-empty? true
            :value (:max-length @search-params)
            :on-change #(dispatch [:set-params-and-search-offerings-main-search {:max-length %2} {:add-to-query? true}])}
           max-length-text-field-props)]]])))

(def offerings-order-by-options
  [[[:created-on :desc] "Newest"]
   [[:bid-count :desc] "Most Active"]
   [[:price :desc] "Most Expensive"]
   [[:price :asc] "Cheapest"]
   [[:end-time :asc] "Ending Soon"]])

(defn order-by-select-field []
  (let [search-params (subscribe [:search-params/offerings-main-search])]
    (fn [{:keys [:on-change] :as props}]
      [ui/select-field
       (r/merge-props
         {:full-width true
          :hint-text "Order By"
          :value (str [(first (:order-by-columns @search-params))
                       (first (:order-by-dirs @search-params))])
          :on-change (fn [e index]
                       (let [[order-by-column order-by-dir] (first (nth offerings-order-by-options index))]
                         (dispatch [:set-params-and-search-offerings-main-search
                                    {:order-by-columns [(name order-by-column)]
                                     :order-by-dirs [(name order-by-dir)]}
                                    {:add-to-query? true}])))}
         props)
       (for [[val text] offerings-order-by-options]
         [ui/menu-item
          {:key (str val)                                   ; hack, because material-ui selectfield
           :value (str val)                                 ; doesn't support non-primitive values
           :primary-text text}])])))

(defn reset-search-icon-button []
  [ui/icon-button
   {:tooltip "Reset Search"
    :on-click #(dispatch [:district0x.location/set-query ""])}
   (icons/filter-remove)])

(defn search-params-panel []
  [paper
   {:inner-style {:padding-top 0}}
   [row-with-cols
    {:bottom "xs"
     :between "xs"}
    [col
     {:md 5}
     [keyword-text-field]]
    [col
     {:md 3}
     [row
      {:bottom "xs"}
      [keyword-position-select-field]]]
    [col
     {:md 4}
     [row
      {:bottom "xs"}
      [saved-searches-select-field]
      [save-search-button]]]]
   [row-with-cols
    {:style styles/margin-top-gutter-less}
    [col
     {:md 4}
     [offering-type-checkbox-group]]
    [col
     {:md 4}
     [name-level-checkbox-group]]
    [col
     {:md 4}
     [exclude-chars-checkbox-group]]]
   [row-with-cols
    {:bottom "xs"}
    [col
     {:md 4}
     [price-text-fields]]
    [col
     {:md 4}
     [length-text-fields]]
    [col
     {:md 4}
     [row
      {:bottom "xs"}
      [order-by-select-field
       {:style styles/offerings-order-by-select-field}]
      [reset-search-icon-button]
      ]]]])

(defn search-params-drawer-mobile []
  (let [open? (subscribe [:offerings-search-params-drawer-open?])]
    (fn []
      [ui/drawer
       {:open-secondary true
        :open @open?
        :on-request-change #(dispatch [:set-offerings-search-params-drawer %])}
       [:div
        {:style styles/offering-search-params-drawer-mobile}
        [row
         [keyword-position-select-field]]
        [row
         [price-text-fields]]
        [row
         [length-text-fields]]
        [row
         {:style styles/margin-top-gutter}
         [offering-type-checkbox-group]]
        [row
         {:style styles/margin-top-gutter}
         [name-level-checkbox-group]]
        [row
         {:style styles/margin-top-gutter}
         [exclude-chars-checkbox-group]]
        [row
         {:style styles/margin-top-gutter}
         [ui/raised-button
          {:full-width true
           :primary true
           :label "Close"
           :on-click #(dispatch [:set-offerings-search-params-drawer false])}]]
        [row
         {:style styles/margin-top-gutter-less}
         [ui/flat-button
          {:full-width true
           :label "Reset"
           :on-click #(dispatch [:district0x.location/set-query ""])}]]]])))

(defn search-params-panel-mobile []
  [paper
   {:inner-style {:padding-top 0}}
   [search-params-drawer-mobile]
   [keyword-text-field]
   [order-by-select-field]
   [row
    [saved-searches-select-field]
    [save-search-button]]])


(defn offerings-search-results []
  (let [search-results (subscribe [:search-results/offerings-main-search])]
    (fn []
      (let [{:keys [:items :loading? :params]} @search-results]
        [paper
         {:inner-style {:padding-bottom 0
                        :padding-left 0
                        :padding-right 0}}
         [infinite-list
          {:initial-load-limit constants/infinite-lists-init-load-limit
           :next-load-limit constants/infinite-lists-next-load-limit
           :offset (:offset params)
           :loading? loading?
           :collapsed-item-height styles/offering-list-item-height
           :on-infinite-load #(dispatch [:set-params-and-search-offerings-main-search
                                         {:offset %
                                          :limit constants/infinite-lists-next-load-limit}])}
          (doall
            (for [[i offering] (medley/indexed items)]
              [offering-list-item
               {:key i
                :offering offering
                :show-bid-count? true
                :show-time-left? true}]))]]))))

(defmethod page :route.offerings/search []
  (let [xs-sm? (subscribe [:district0x/window-xs-sm-width?])]
    (fn []
      [side-nav-menu-layout
       [center-layout
        (if @xs-sm?
          [search-params-panel-mobile]
          [search-params-panel])
        [offerings-search-results]]])))
