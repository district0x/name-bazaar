(ns name-bazaar.ui.pages.home-page
  (:require
    [clojure.string :as string]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.utils :refer [format-eth-with-code]]
    [medley.core :as medley]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [ensure-registrar-root offerings-newest-url offerings-most-active-url offerings-ending-soon-url valid-ens-name? path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn- nav-to-ens-record-detail [hashroutes? name]
  (when-not (empty? name)
    (dispatch [:district0x.location/nav-to hashroutes? :route.ens-record/detail
               {:ens.record/name (ensure-registrar-root name)}
               constants/routes])))

(defn transform-search-results [items]
  (->> items
    (map (fn [{:keys [:offering/name :offering/price :offering/address] :as offering}]
           (when (and name price)
             {:title name :price (format-eth-with-code price) :id address})))
    (remove nil?)))

(defn search-bar []
  (let [search-name (r/atom "")
        search-results (subscribe [:offerings/home-page-autocomplete])
        hashroutes? @(subscribe [:district0x.browsing/hashroutes?])]
    (fn []
      (let [{:keys [:items :loading?]} @search-results]
        [ui/Search
         {:class :keyword-search
          :value @search-name
          :show-no-results false
          :placeholder "Enter Keyword"
          :results (transform-search-results items)
          :icon (r/as-element [:i.icon.magnifier
                               {:on-click #(nav-to-ens-record-detail hashroutes? @search-name)}])
          :on-key-press (fn [e]
                          (when (= (aget e "key") "Enter")
                            (nav-to-ens-record-detail hashroutes? @search-name)))
          :on-result-select (fn [_ data]
                              (dispatch [:district0x.location/nav-to
                                         :route.offerings/detail
                                         {:offering/address (aget data "result" "id")}
                                         constants/routes]))
          :on-search-change (fn [_ data]
                              (let [value (aget data "value")]
                                (when (valid-ens-name? value)
                                  (reset! search-name value)
                                  (dispatch [:offerings.home-page-autocomplete/search {:name @search-name}]))))}]))))

(defn offerings-column [{:keys [:title :icon-class :offerings :show-more-href]}]
  [ui/Segment
   [:div.title-container
    [:i.icon
     {:class icon-class}]
    [:div.title title]]
   (doall
     (for [[i offering] (medley/indexed offerings)]
       [offering-list-item
        {:key i
         :offering offering
         :disable-expand? true
         :mobile? true
         :on-click #(dispatch [:district0x.location/nav-to :route.offerings/detail offering constants/routes])}]))
   [:div
    [:a.show-more
     {:href show-more-href}
     "Show more"]]])

(defn offerings-columns []
  (let [offerings-newest (subscribe [:offerings/home-page-newest])
        offerings-most-active (subscribe [:offerings/home-page-most-active])
        offerings-ending-soon (subscribe [:offerings/home-page-ending-soon])
        hashroutes? @(subscribe [:district0x.browsing/hashroutes?])]
    (fn []
      [ui/Grid
       {:class :offerings-grid
        :columns 3
        :centered true}
       (for [props [{:title "Latest"
                     :offerings (:items @offerings-newest)
                     :show-more-href (offerings-newest-url hashroutes?)
                     :icon-class "leaf"}
                    {:title "Most Active"
                     :offerings (:items @offerings-most-active)
                     :show-more-href (offerings-most-active-url hashroutes?)
                     :icon-class "pulse"}
                    {:title "Ending Soon"
                     :offerings (:items @offerings-ending-soon)
                     :show-more-href (offerings-ending-soon-url hashroutes?)
                     :icon-class "flag"}]]
         [ui/GridColumn
          {:key (:title props)
           :widescreen 3
           :large-screen 4
           :computer 5
           :tablet 8
           :mobile 16
           :text-align "center"
           :class :offering-column}
          [offerings-column props]])])))

(defn namebazaar-logo [hashroutes?]
  [:a
   {:href (path-for hashroutes? :route/home)}
   [:img.logo
    {:src "./images/logo@2x.png"}]])

(defn app-pages [hashroutes?]
  [ui/Grid
   {:columns 1
    :centered true
    :class :app-page-link-grid}
   [ui/GridColumn
    {:widescreen 5
     :large-screen 8
     :computer 9
     :tablet 12
     :mobile 16
     :text-align "center"}
    [:div.app-page-button-links
     [:a.ui.button
      {:href (path-for hashroutes? :route.offerings/search)}
      "View Offerings"
      [:i.hand.icon]]

     [:a.ui.button
      {:href (path-for hashroutes? :route.offerings/create)}
      "Create Offering"
      [:i.price-tag.icon]]

     [:a.ui.button
      {:href (path-for hashroutes? :route/how-it-works)}
      "How It Works"
      [:i.book.icon]]]]])

(defn app-headline []
  [ui/Grid
   {:columns 1
    :centered true}
   [ui/GridColumn
    {:computer 7
     :tablet 12
     :mobile 15
     :text-align :center}
    [:h1.intro-headline
     "A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service."]]])

(defn footer []
  [ui/Grid
   {:text-align :center
    :class :footer
    :vertical-align :middle}
   [:span.footer-logo]
   [:h3.part-of-district0x
    "Part of the "
    [:a {:href "https://district0x.io" :target :_blank}
     "district0x Network"]]])

(defmethod page :route/home []
  (let [xs-sm? (subscribe [:district0x.screen-size/max-tablet?])
        hashroutes? @(subscribe [:district0x.browsing/hashroutes?])]
    (fn []
      [:div.home-page
       [:div.top-segment
        [namebazaar-logo hashroutes?]]
       [app-headline]
       [ui/Grid
        {:columns 1
         :centered true}
        [ui/GridColumn
         {:widescreen 7
          :large-screen 9
          :computer 12
          :tablet 12
          :mobile 15}
         [search-bar]]]
       [app-pages hashroutes?]
       [offerings-columns]
       [footer]])))
