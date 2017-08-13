(ns name-bazaar.ui.pages.home-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [clojure.string :as string]))

(defmethod page :route/home []
  (let [search-form (subscribe [:search-form/home-page-search])]
    (fn []
      (let [{:keys [:data :errors]} @search-form
            {:keys [:offering/name]} data
            {:keys [:items]} @(subscribe [:search-results/offerings data])]
        [:div "Aaa"]
        [center-layout
         [row
          [ui/auto-complete
           {:dataSource (map :offering/name items)
            :full-width true
            :floating-label-text "Search"
            :search-text name
            :on-new-request (fn [name]
                              (let [name (if-not (string/includes? name ".") (str name ".eth") name)]
                                (dispatch [:district0x.location/nav-to :route.ens-record/detail {:ens.record/name name}])))
            :on-update-input (fn [text]
                               (dispatch [:search/home-page-search {:offering/name text}])
                               (dispatch [:district0x.form/set-value :search-form/home-page-search :offering/name text]))}]
          [:div
           {:style styles/margin-top-gutter-less}
           [row
            [misc/a {:route :route.offerings/search} "View current offerings"]]
           [row
            [misc/a {:route :route.offering/create} "Create new offering"]]]]]))))