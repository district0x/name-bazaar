(ns name-bazaar.ui.components.offering-request.list-item
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col]]
    [district0x.ui.utils :as d0x-ui-utils :refer [pluralize]]
    [name-bazaar.ui.components.ens-name-details :refer [ens-name-details]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn offering-request-list-item-header []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering-request]}]
      (let [{:keys [:offering-request/node :offering-request/name :offering-request/requesters-count]} offering-request]
        [:div
         {:style (styles/search-results-list-item @xs?)}
         (when-not node
           [list-item-placeholder])
         [row-with-cols
          {:style (merge styles/search-results-list-item-header
                         (if node styles/opacity-1 styles/opacity-0))
           :between "sm"
           :middle "sm"}
          [col
           {:xs 12 :sm 5}
           [:div
            {:style styles/list-item-ens-record-name}
            name]]
          [col
           {:xs 6 :sm 3}
           [:div
            {:style (styles/offering-requests-list-item-count @xs?)}
            requesters-count (pluralize " request" requesters-count)]]]]))))

(defn offering-request-list-item []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering-request :expanded? :on-expand :key]}]
      (let [{:keys [:offering-request/node :offering-request/name]} offering-request]
        [expandable-list-item
         {:index key
          :on-expand #(dispatch [:offering-requests.list-item/expanded offering-request])
          :collapsed-height (styles/search-results-list-item-height @xs?)
          :expanded-height (styles/offering-request-list-item-expanded-height @xs?)
          :expand-disabled? (not node)}
         [offering-request-list-item-header
          {:offering-request offering-request}]
         [ens-name-details
          {:show-name-detail-link? true
           :ens.record/name name}]]))))
