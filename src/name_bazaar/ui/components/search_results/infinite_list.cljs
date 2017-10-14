(ns name-bazaar.ui.components.search-results.infinite-list
  (:require
    [medley.core :as medley]
    [name-bazaar.ui.components.infinite-list :refer [infinite-list]]
    [name-bazaar.ui.components.loading-placeholders :refer [list-item-placeholder]]
    [name-bazaar.ui.components.offering-request.list-item :refer [offering-request-list-item-header]]
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn search-results-infinite-list []
  (let [mobile? (subscribe [:district0x.screen-size/mobile?])]
    (fn [{:keys [:no-items-text] :as props} list-items]
      (let [collapsed-height (constants/infinite-list-collapsed-item-height @mobile?)]
        [infinite-list
         (r/merge-props
           {:initial-load-limit constants/infinite-lists-init-load-limit
            :next-load-limit constants/infinite-lists-next-load-limit
            :collapsed-item-height collapsed-height
            :loading-spinner-delegate (r/as-element [offering-request-list-item-header
                                                     {:style {:height collapsed-height}}])
            :no-items-element (r/as-element [:div.no-items-text no-items-text])}
           (dissoc props :no-items-text :list-header))
         list-items]))))