(ns name-bazaar.ui.pages.home-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.utils :as d0x-ui-utils]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.components.misc :as misc]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [ensure-registrar-root offerings-newest-url offerings-most-active-url offerings-ending-soon-url]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [medley.core :as medley]))

(defn- nav-to-ens-record-detail [name]
  (when-not (empty? name)
    (dispatch [:district0x.location/nav-to :route.ens-record/detail
               {:ens.record/name (ensure-registrar-root name)}
               constants/routes])))


(defn autocomplete-search-bar []
  (let [search-name (r/atom "")
        search-results (subscribe [:offerings/home-page-autocomplete])]
    (fn []
      (let [{:keys [:items]} @search-results]
        ;; Styling teporary here until we decide on design
        [paper
         {:style {:width "100%"
                  :height 48
                  :display :flex
                  :justifyContent "space-between"
                  :padding 0
                  :margin-bottom 0}}
         [:div
          {:style (merge styles/full-width
                         styles/margin-left-gutter-less)}
          [ui/auto-complete
           {:dataSource (d0x-ui-utils/map->data-source items :offering/address :offering/name)
            :dataSourceConfig d0x-ui-utils/default-data-source-config
            :full-width true
            :underline-show false
            :hint-text "Enter Keyword"
            :search-text @search-name
            :on-new-request (fn [value]
                              (let [value (js->clj value :keywordize-keys true)]
                                (cond
                                  (string? value)
                                  (nav-to-ens-record-detail value)

                                  (map? value)
                                  (dispatch [:district0x.location/nav-to
                                             :route.offerings/detail
                                             {:offering/address (:value value)}
                                             constants/routes]))))
            :on-update-input (fn [value]
                               (reset! search-name value)
                               (when (>= (count value) 3)
                                 (dispatch [:offerings.home-page-autocomplete/search {:name @search-name}])))}]]
         [ui/icon-button
          {:style {:opacity 0.54}
           :on-click #(nav-to-ens-record-detail @search-name)}
          (icons/magnify)]]))))

(defn info-box []
  [col
   {:xs 12 :sm 6 :md 4
    :style (merge styles/margin-bottom-gutter
                  styles/text-left)}
   [:div "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum condimentum nunc justo,
    eu viverra leo venenatis malesuada. Proin non metus turpis. Curabitur non nisi est."]])

(defn info-boxes []
  [row-with-cols
   {:style (merge styles/margin-top-gutter
                  {:font-size "1.15em"})
    :center "xs"}
   [info-box]
   [info-box]
   [info-box]])

(defn offering-box [{:keys [:title :offerings :show-more-href]}]
  [col
   {:xs 12 :sm 6 :md 4
    :style (merge styles/margin-bottom-gutter
                  styles/text-left)}
   [paper
    {:style {:padding-left 0
             :padding-right 0}}
    [:h2
     {:style (merge styles/margin-bottom-gutter-less
                    styles/margin-left-gutter-mini)}
     title]
    (doall
      (for [[i offering] (medley/indexed offerings)]
        [offering-list-item
         {:key i
          :offering offering
          :expand-disabled? true
          :xs? true
          :on-click #(dispatch [:district0x.location/nav-to :route.offerings/detail offering constants/routes])}]))
    [:div
     {:style (merge styles/text-right
                    styles/margin-right-gutter-mini
                    styles/margin-top-gutter-mini)}
     [:a
      {:href show-more-href}
      "Show more"]]]])

(defn offering-boxes []
  (let [offerings-newest (subscribe [:offerings/home-page-newest])
        offerings-most-active (subscribe [:offerings/home-page-most-active])
        offerings-ending-soon (subscribe [:offerings/home-page-ending-soon])]
    (fn []
      [row-with-cols
       {:style (merge styles/margin-top-gutter styles/full-width)
        :center "xs"}
       [offering-box
        {:title "Latest"
         :offerings (:items @offerings-newest)
         :show-more-href offerings-newest-url}]
       [offering-box
        {:title "Most Active"
         :offerings (:items @offerings-most-active)
         :show-more-href offerings-most-active-url}]
       [offering-box
        {:title "Ending Soon"
         :offerings (:items @offerings-ending-soon)
         :show-more-href offerings-ending-soon-url}]])))

(defmethod page :route/home []
  (let [xs-sm? (subscribe [:district0x/window-xs-sm-width?])]
    (fn []
      [row-with-cols {:center "xs"}
       [col {:xs 12 :md 11 :lg 10 :style styles/text-left}
        [row
         {:style (merge {:padding-top 100}
                        (when @xs-sm?
                          {:padding-left styles/desktop-gutter-less
                           :padding-right styles/desktop-gutter-less}))}
         [:h1
          ;; Styling teporary here until we decide on design
          {:style (merge styles/text-center
                         styles/full-width
                         styles/margin-bottom-gutter
                         {:font-size "5em"
                          :font-weight 300})}
          "Name Bazaar"]
         [:h1
          {:style (merge styles/text-center
                         styles/margin-bottom-gutter)}
          "A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service."]
         [autocomplete-search-bar]
         [row-with-cols
          {:end "xs"
           :style (merge styles/margin-top-gutter-less
                         styles/full-width)}
          [col
           {:xs 12}
           [misc/a {:route :route.offerings/search} "View Offerings"]]
          [col
           {:xs 12}
           [misc/a {:route :route.offerings/create} "Create new offering"]]]
         [info-boxes]
         [offering-boxes]]]])))