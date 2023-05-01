(ns name-bazaar.ui.components.infinite-list
  (:require
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    ["react-infinite" :as Infinite]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(def react-infinite (r/adapt-react-class Infinite))

(defn expandable-list-item-body []
  (let [visible? (r/atom false)]
    (fn [{:keys [:index] :as props} & children]
      (into [:div.body
             {:class (when @visible? :opacity-1)
              :ref (fn [el]
                     (reset! visible? true)
                     (when el
                       (dispatch [:infinite-list.item/set-expanded-height index (aget el "clientHeight")])))}]
            children))))

(defn expandable-list-item-header []
  (fn [{:keys [:index :collapsed-height :on-collapse :on-expand :disable-expand? :on-click :href] :as props}
       & children]
    (let [expanded? @(subscribe [:infinite-list.item/expanded? index])]
      (into
        [:a.header
         {:style {:height collapsed-height}
          :href href                                        ;; Mostly for search engines
          :data-pushy-ignore true
          :on-click (fn [e]
                      (.preventDefault e)
                      (when (fn? on-click)
                        (on-click index))
                      (when-not disable-expand?
                        (if expanded?
                          (do
                            (when (fn? on-collapse)
                              (on-collapse))
                            (dispatch [:infinite-list.item/collapse index]))
                          (do
                            (when (fn? on-expand)
                              (on-expand))
                            (dispatch [:infinite-list.item/initialize-expand index])))))}]
        children))))

(defn expandable-list-item [{:keys [:index :on-collapse :on-expand :collapsed-height :disable-expand?
                                    :on-click :href]}
                            header body]
  [:div.expandable-list-item
   [expandable-list-item-header
    {:index index
     :on-collapse on-collapse
     :on-expand on-expand
     :disable-expand? disable-expand?
     :collapsed-height collapsed-height
     :on-click on-click
     :href href}
    header]
   (when (and (not disable-expand?) @(subscribe [:infinite-list.item/expanded? index]))
     [expandable-list-item-body
      {:index index}
      body])])

(defn scroll-to-top []
  (let [stuck? (r/atom false)]
    (fn [{:keys [:context] :as props}]
      [ui/Sticky
       (r/merge-props
         {:class "infinite-list-scroll-to-top-sticky"
          :on-stick (fn []
                      (when-not @stuck?
                        (reset! stuck? true)))
          :on-unstick (fn []
                        (when @stuck?
                          (reset! stuck? false)))}
         props)
       [:div
        (merge
          {:class (str "infinite-list-scroll-to-top "
                       (when @stuck? "stuck"))
           :on-click #(dispatch [:district0x.window/scroll-to-top])}
          (when context
            {:style {:width (aget context "clientWidth")}}))
        "⌃"]])))

(defn infinite-list []
  (let [container-ref (r/atom nil)
        load-disabled? (r/atom false)]
    (fn [{:keys [:initial-load-limit :next-load-limit :offset :loading? :loading-spinner-delegate
                 :collapsed-item-height :on-infinite-load :on-initial-load :on-next-load
                 :total-count :no-items-element] :as props}
         list-items]
      (let [all-items-loaded? (= total-count (count list-items))]
        (if (and list-items all-items-loaded? (zero? (count list-items)) no-items-element)
          no-items-element
          [:div
           {:ref (fn [el]
                   (when (and el (not @container-ref))
                     (reset! container-ref el)))}
           [scroll-to-top
            {:context @container-ref}]
           [react-infinite
            (r/merge-props
              {:class "infinite-list"
               :element-height @(subscribe [:infinite-list/items-heights (count list-items) collapsed-item-height])
               :use-window-as-scroll-container true
               :infinite-load-begin-edge-offset -250
               :is-infinite-loading (and loading?
                                         (not all-items-loaded?)
                                         (not @load-disabled?))
               :on-infinite-load (fn []
                                   (when (and (not loading?)
                                              (not all-items-loaded?)
                                              (not @load-disabled?))
                                     (reset! load-disabled? true) ;; To prevent too many firings
                                     (js/setTimeout #(reset! load-disabled? false) 300)
                                     (if (and (zero? offset)
                                              (empty? list-items))
                                       (when (fn? on-infinite-load)
                                         (on-infinite-load 0 initial-load-limit))
                                       (let [next-offset (if (zero? offset) initial-load-limit (+ offset next-load-limit))]
                                         (when (= next-offset (count list-items))
                                           (when (fn? on-initial-load)
                                             (on-infinite-load next-offset next-load-limit))
                                           (when (fn? on-next-load)
                                             (on-next-load (min next-offset total-count) next-load-limit)))))))
               :loading-spinner-delegate (when (and loading? (not all-items-loaded?))
                                           loading-spinner-delegate)}
              (dissoc props :initial-load-limit :next-load-limit :offset :loading? :loading-spinner-delegate
                      :collapsed-item-height :on-infinite-load))
            list-items]
           ;; For bots only
           (let [next-offset (+ offset next-load-limit)]
             (when (< next-offset total-count)
               [:a.infinite-list-next-page-link
                {:href @(subscribe [:infinite-list/next-page-url next-offset])}]))])))))
