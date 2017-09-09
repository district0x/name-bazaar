(ns name-bazaar.ui.components.search-results.infinite-list
  (:require
    [medley.core :as medley]
    [name-bazaar.ui.components.infinite-list :refer [infinite-list]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn search-results-infinite-list []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:no-items-text] :as props} list-items]
      [infinite-list
       (r/merge-props
         {:initial-load-limit constants/infinite-lists-init-load-limit
          :next-load-limit constants/infinite-lists-next-load-limit
          :collapsed-item-height (styles/search-results-list-item-height @xs?)
          :loading-spinner-delegate (r/as-element [:div
                                                   {:style (styles/search-results-list-item-placeholder @xs?)}
                                                   [list-item-placeholder]])
          :no-items-element (r/as-element [:div
                                           {:style styles/search-results-no-items}
                                           no-items-text])}
         (dissoc props :no-items-text))
       list-items])))