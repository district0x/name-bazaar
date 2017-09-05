(ns name-bazaar.ui.pages.home-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.utils :as d0x-ui-utils]
    [name-bazaar.ui.components.misc :as misc]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn autocomplete-search-bar []
  (let [search-params (r/atom {:offering/name ""})
        search-results (subscribe [:offerings/home-page-autocomplete])]
    (fn []
      (let [{:keys [:items]} @search-results]
        [ui/auto-complete
         {:dataSource (d0x-ui-utils/map->data-source items :offering/address :offering/name)
          :dataSourceConfig d0x-ui-utils/default-data-source-config
          :full-width true
          :floating-label-text "Search"
          :search-text (:offering/name @search-params)
          :on-new-request (fn [value]
                            (let [value (js->clj value :keywordize-keys true)]
                              (cond
                                (string? value)
                                (let [name (if-not (string/ends-with? value ".eth") (str value ".eth") value)]
                                  (dispatch [:district0x.location/nav-to
                                             :route.ens-record/detail
                                             {:ens.record/name name}
                                             constants/routes]))

                                (map? value)
                                (dispatch [:district0x.location/nav-to
                                           :route.offerings/detail
                                           {:offering/address (:value value)}
                                           constants/routes]))))
          :on-update-input (fn [value]
                             (swap! search-params assoc :offering/name value)
                             (when (>= (count value) 3)
                               (dispatch [:offerings.home-page-autocomplete/search @search-params])))}]))))

(defmethod page :route/home []
  [center-layout
   [row
    [autocomplete-search-bar]
    [:div
     {:style styles/margin-top-gutter-less}
     [row
      [misc/a {:route :route.offerings/search} "Search Offerings"]]
     [row
      [misc/a {:route :route.offerings/create} "Create new offering"]]]]])