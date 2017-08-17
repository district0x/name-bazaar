(ns name-bazaar.ui.pages.home-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.utils :as d0x-ui-utils]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn autocomplete-search-bar []
  (let [search-params (r/atom {:offering/node-owner? true
                               :offering/name ""
                               :limit 5})]
    (fn []
      (let [{:keys [:items]} @(subscribe [:search-results/offerings @search-params])]
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
                                  (dispatch [:district0x.location/nav-to :route.ens-record/detail {:ens.record/name name}]))

                                (map? value)
                                (dispatch [:district0x.location/nav-to :route.offering/detail {:offering/address
                                                                                               (:value value)}]))))
          :on-update-input (fn [value]
                             (swap! search-params assoc :offering/name value)
                             (when (>= (count value) 3)
                               (dispatch [:search/offerings-debounced @search-params])))}]))))


(defmethod page :route/home []
  [center-layout
   [row
    [autocomplete-search-bar]
    [:div
     {:style styles/margin-top-gutter-less}
     [row
      [misc/a {:route :route.offerings/search} "View current offerings"]]
     [row
      [misc/a {:route :route.offering/create} "Create new offering"]]]]])