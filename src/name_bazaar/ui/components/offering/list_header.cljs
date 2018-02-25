(ns name-bazaar.ui.components.offering.list-header
  (:require
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [subscribe dispatch]]
    [soda-ash.core :as ui]))

(defn offering-list-header []
  (let [mobile? (subscribe [:district0x.window.size/mobile?])]
    (fn [{:keys [:show-time-ago? :show-sold-for?] :as props}]
      [:div.search-results-list-item.list-header.opacity-1
       [:div.bids "Bids"]
       [:div.time (if show-time-ago?
                    "Time Ago"
                    "Time Left")]
       (when show-sold-for?
         [:div.sold "Sold For"])])))
