(ns name-bazaar.ui.pages.offering-requests-search-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col paper page]]
    [district0x.ui.components.text-field :refer [text-field]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code]]
    [medley.core :as medley]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.components.offering-request.list-item :refer [offering-request-list-item]]
    [name-bazaar.ui.components.search-fields.keyword-position-select-field :refer [keyword-position-select-field]]
    [name-bazaar.ui.components.search-fields.keyword-text-field :refer [keyword-text-field]]
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn offering-requests-keyword-text-field []
  (let [search-params (subscribe [:offering-requests.main-search/params])]
    (fn []
      [keyword-text-field
       {:value (:name @search-params)
        :on-change #(dispatch [:district0x.location/add-to-query {:name %2}])}])))

(defn offering-requests-keyword-position-select-field []
  (let [search-params (subscribe [:offering-requests.main-search/params])]
    (fn []
      [keyword-position-select-field
       {:value (:name-position @search-params)
        :on-change #(dispatch [:district0x.location/add-to-query {:name-position %3}])}])))

(defn search-params-panel []
  [paper
   [row-with-cols
    {:bottom "xs"}
    [col
     {:xs 12 :sm 9}
     [offering-requests-keyword-text-field]]
    [col
     {:xs 12 :sm 3}
     [row
      {:bottom "xs"}
      [offering-requests-keyword-position-select-field]]]]])

(defn offering-requests-search-results []
  (let [search-results (subscribe [:offering-requests/main-search])]
    (fn []
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [paper
         {:style styles/search-results-paper}
         [search-results-infinite-list
          {:total-count total-count
           :offset (:offset params)
           :loading? loading?
           :no-items-text "No offering requests found matching your search criteria"
           :on-next-load (fn [offset limit]
                           (dispatch [:offering-requests.main-search/set-params-and-search
                                      {:offset offset :limit limit}
                                      {:append? true}]))}
          (doall
            (for [[i offering-request] (medley/indexed items)]
              [offering-request-list-item
               {:key i
                :offering-request offering-request}]))]]))))

(defmethod page :route.offering-requests/search []
  [side-nav-menu-center-layout
   [search-params-panel]
   [offering-requests-search-results]])
