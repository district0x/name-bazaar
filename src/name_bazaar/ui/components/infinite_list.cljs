(ns name-bazaar.ui.components.infinite-list
  (:require
    [react-infinite]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col]]
    [reagent.core :as r]))

(def react-infinite (r/adapt-react-class js/Infinite))
(def expandable-item-style {:width "100%" :border-bottom "0.5px solid #ddd"})
(def expandable-item-header-style {:width "100%" :cursor :pointer})
(def expandable-item-body-style {:overflow :hidden :transition "height 0.15s cubic-bezier(0.77, 0, 0.175, 1)"})

(defn expandable-list-item-body [{:keys [:index :collapsed-height] :as props} & children]
  (into
    [:div
     {:style (merge expandable-item-body-style
                    {:height @(subscribe [:infinite-list.item/expanded-body-height index collapsed-height])})}]
    (when @(subscribe [:infinite-list.item/expanded? index]) ;; This is important for performance reasons
      children)))

(defn expandable-list-item-header []
  (fn [{:keys [:index :expanded-height :collapsed-height :on-collapse :on-expand :expand-disabled? :on-click] :as props}
       & children]
    (let [expanded? @(subscribe [:infinite-list.item/expanded? index])]
      (into
        [row
         {:middle "xs"
          :style (merge expandable-item-header-style
                        {:height collapsed-height})
          :on-click (fn []
                      (when (fn? on-click)
                        (on-click index))
                      (when-not expand-disabled?
                        (if expanded?
                          (do
                            (when (fn? on-collapse)
                              (on-collapse))
                            (dispatch [:infinite-list.item/collapse index]))
                          (do
                            (when (fn? on-expand)
                              (on-expand))
                            (dispatch [:infinite-list.item/expand index expanded-height])))))}]
        children))))

(defn expandable-list-item [{:keys [:index :on-collapse :on-expand :expanded-height :collapsed-height :expand-disabled?
                                    :on-click]}
                            header body]
  [:div
   {:style expandable-item-style}
   [expandable-list-item-header
    {:index index
     :expanded-height expanded-height
     :on-collapse on-collapse
     :on-expand on-expand
     :expand-disabled? expand-disabled?
     :collapsed-height collapsed-height
     :on-click on-click}
    header]
   (when-not expand-disabled?
     [expandable-list-item-body
      {:index index
       :collapsed-height collapsed-height}
      body])])

(defn infinite-list [{:keys [:initial-load-limit :next-load-limit :offset :loading? :loading-spinner-delegate
                             :collapsed-item-height :on-infinite-load :on-initial-load :on-next-load
                             :total-count :no-items-element] :as props}
                     list-items]
  (let [all-items-loaded? (= total-count (count list-items))]
    (if (and list-items all-items-loaded? (zero? (count list-items)) no-items-element)
      no-items-element
      [react-infinite
       (r/merge-props
         {:element-height @(subscribe [:infinite-list/items-heights (count list-items) collapsed-item-height])
          :use-window-as-scroll-container true
          :infinite-load-begin-edge-offset -250
          :is-infinite-loading (and loading? (not all-items-loaded?))
          :on-infinite-load (fn []
                              (when (and (not loading?)
                                         (not all-items-loaded?))
                                (if (and (zero? offset)
                                         (empty? list-items))
                                  (do
                                    (when (fn? on-infinite-load)
                                      (on-infinite-load 0 initial-load-limit))
                                    (when on-initial-load
                                      (on-initial-load 0 initial-load-limit)))
                                  (let [next-offset (if (zero? offset) initial-load-limit (+ offset next-load-limit))]
                                    (when (fn? on-initial-load)
                                      (on-infinite-load next-offset next-load-limit))
                                    (when (fn? on-next-load)
                                      (on-next-load next-offset next-load-limit))))))
          :loading-spinner-delegate (when (and loading? (not all-items-loaded?))
                                      loading-spinner-delegate)}
         (dissoc props :initial-load-limit :next-load-limit :offset :loading? :loading-spinner-delegate
                 :collapsed-item-height :on-infinite-load))
       list-items])))
