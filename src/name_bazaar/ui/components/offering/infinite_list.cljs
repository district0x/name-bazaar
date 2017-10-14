(ns name-bazaar.ui.components.offering.infinite-list
  (:require
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.components.offering.list-header :refer [offering-list-header]]))

(defn offering-infinite-list [{:keys [:header-props] :as props} & children]
  [:div.infinite-list-container
   [offering-list-header header-props]
   (into [search-results-infinite-list (dissoc props :header-props)] children)])
