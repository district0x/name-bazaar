(ns name-bazaar.ui.components.infinite-list
  (:require
    [cljsjs.react-infinite]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def react-infinite (r/adapt-react-class js/Infinite))
(def expandable-item-style {:width "100%" :border-bottom "0.5px solid #ddd"})
(def expandable-item-header-style {:width "100%" :cursor :pointer})
(def expandable-item-body-style {:overflow :hidden :transition "height 0.15s cubic-bezier(0.77, 0, 0.175, 1)"})

(defn expandable-list-item-body [{:keys [:index :collapsed-height] :as props} & children]
  (into
    [:div
     {:style (merge expandable-item-body-style
                    {:height @(subscribe [:infinite-list/expanded-item-body-height index collapsed-height])})}]
    children))

(defn expandable-list-item-header []
  (fn [{:keys [:index :expanded-height :collapsed-height :on-collapse :on-expand] :as props} & children]
    (let [expanded? @(subscribe [:infinite-list/item-expanded? index])]
      (into
        [:div
         {:style (merge expandable-item-header-style
                        {:height collapsed-height})
          :on-click (fn []
                      (if expanded?
                        (do
                          (on-collapse)
                          (dispatch [:infinite-list/collapse-item index]))
                        (do
                          (on-expand)
                          (dispatch [:infinite-list/expand-item index expanded-height]))))}]
        children))))

(defn expandable-list-item [{:keys [:index :on-collapse :on-expand :expanded-height :collapsed-height]}
                            header body]
  [:div
   {:style expandable-item-style}
   [expandable-list-item-header
    {:index index
     :expanded-height expanded-height
     :on-collapse on-collapse
     :on-expand on-expand}
    header]
   [expandable-list-item-body
    {:index index
     :collapsed-height collapsed-height}
    body]])

(defn infinite-list [{:keys [:initial-load-limit :next-load-limit :offset :loading? :loading-spinner-delegate
                             :collapsed-item-height :on-infinite-load] :as props}
                     list-items]
  [react-infinite
   (r/merge-props
     {:element-height @(subscribe [:infinite-list/items-heights (count list-items) collapsed-item-height])
      :use-window-as-scroll-container true
      :infinite-load-begin-edge-offset -250
      :is-infinite-loading loading?
      :on-infinite-load (fn []
                          (let [new-offset (+ offset (if (zero? offset) initial-load-limit next-load-limit))]
                            (when (= new-offset (count list-items))
                              (on-infinite-load new-offset))))
      :loading-spinner-delegate (when loading?
                                  loading-spinner-delegate)}
     (dissoc props :initial-load-limit :next-load-limit :offset :loading? :loading-spinner-delegate
             :collapsed-item-height :on-infinite-load))
   list-items])
