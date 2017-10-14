(ns name-bazaar.ui.events.infinite-list-events
  (:require
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]))

(reg-event-fx
  :infinite-list.item/initialize-expand
  interceptors
  (fn [{:keys [:db]} [key]]
    {:db (assoc-in db [:infinite-list :expanded-items key :height] 1)})) ;; Cannot be set to zero

(reg-event-fx
  :infinite-list.item/set-expanded-height
  interceptors
  (fn [{:keys [:db]} [key height]]
    {:db (assoc-in db [:infinite-list :expanded-items key :height] height)}))

(reg-event-fx
  :infinite-list.item/collapse
  interceptors
  (fn [{:keys [:db]} [key]]
    {:db (update-in db [:infinite-list :expanded-items] dissoc key)}))
