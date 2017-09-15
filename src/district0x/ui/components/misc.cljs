(ns district0x.ui.components.misc
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljsjs.react-flexbox-grid]
    [clojure.set :as set]
    [district0x.shared.utils :refer [empty-address?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [current-component-mui-theme parse-props-children create-with-default-props]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [medley.core :as medley]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row-with-cols (r/adapt-react-class js/ReactFlexboxGrid.Row))
(def grid (r/adapt-react-class js/ReactFlexboxGrid.Grid))

(def row (create-with-default-props row-with-cols {:style {:margin-left 0 :margin-right 0}}))

(defmulti page identity)

(defn paper []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [props & children]
      (let [[props children] (parse-props-children props children)
            {:strs [gutter gutterXs]} (current-component-mui-theme "paper")
            gutter (if (and gutterXs @xs?) gutterXs gutter)]
        (into
          [ui/paper
           (r/merge-props
             {:style {:padding gutter
                      :margin-bottom gutter}}
             (dissoc props))]
          children)))))

(defn paper-with-loader []
  (let [xs? (subscribe [:district0x/window-xs-width?])
        connection-error? (subscribe [:district0x/blockchain-connection-error?])]
    (fn [props & children]
      (let [[{:keys [:inner-style] :as props} children] (parse-props-children props children)
            {:strs [gutter gutterXs]} (current-component-mui-theme "paper")
            gutter (if (and gutterXs @xs?) gutterXs gutter)]
        [ui/paper
         (r/merge-props
           {:style {:margin-bottom gutter}}
           (dissoc props :loading? :inner-style))
         [ui/linear-progress {:mode :indeterminate
                              :style {:visibility (if (and (:loading? props)
                                                           (not @connection-error?))
                                                    :visible
                                                    :hidden)}}]
         (into [] (concat [:div {:style (merge {:word-wrap :break-word
                                                :padding gutter}
                                               (:inner-style props))}]
                          children))]))))

(defn center-layout [& children]
  [row-with-cols {:center "xs"}
   (into [col {:xs 12 :md 10 :lg 9 :style {:text-align :left}}]
         children)])

(defn etherscan-link [props & children]
  (let [[{:keys [:address :transaction?] :as props} children] (parse-props-children props children)]
    (if (empty-address? address)
      [:span (if children children address)]
      [:a (r/merge-props
            {:href (d0x-ui-utils/etherscan-url address {:type (if transaction?
                                                                :transaction
                                                                :address)})
             :target :_blank}
            (dissoc props :address :tx-hash :transaction?))
       (if children children address)])))

(defn watch [{:keys [:value :call-on-mount?]
              :or {call-on-mount? true}}]
  (let [old-value (r/atom (when-not call-on-mount? value))]
    (fn [{:keys [:on-change :value]} & childen]
      (when-not (= @old-value value)
        (reset! old-value value)
        (when (fn? on-change)
          (on-change value)))
      (into [:div] childen))))

(defn youtube [props]
  [:iframe
   (r/merge-props
     {:width 560
      :height 315
      :frameBorder 0
      :allowFullScreen true}
     props)])

(defn a [{:keys [:route-params :route :routes] :as props} body]
  [:a
   (r/merge-props
     {:href (when-not (some nil? (vals route-params))
              (d0x-ui-utils/path-for {:route route
                                      :route-params route-params
                                      :routes routes}))
      :on-click #(.stopPropagation %)}
     (dissoc props :route-params :route :routes)) body])

(defn main-panel []
  (let [snackbar (subscribe [:district0x/snackbar])
        ui-disabled? (subscribe [:district0x/ui-disabled?])]
    (fn [{:keys [:mui-theme]} content]
      [ui/mui-theme-provider
       {:mui-theme mui-theme}
       (if-not @ui-disabled?
         [:div
          content
          [ui/snackbar (-> @snackbar
                         (set/rename-keys {:open? :open}))]]
         [:div "UI is disabled"])])))

(defn side-nav-menu []
  (let [lg? (subscribe [:district0x/window-lg-width?])
        drawer-open? (subscribe [:district0x/menu-drawer-open?])
        active-address (subscribe [:district0x/active-address])
        active-page (subscribe [:district0x/active-page])]
    (fn [{:keys [:app-bar-props :contrainer-props :drawer-props :routes :list-props :list-items-props]} & children]
      [ui/drawer
       (r/merge-props
         {:docked @lg?
          :open (or @drawer-open? @lg?)
          :on-request-change #(dispatch [:district0x.menu-drawer/set %])}
         drawer-props)
       (into
         [:div
          (r/merge-props
            {:style {:height "100%"
                     :flex-direction "column"
                     :display "flex"
                     :justify-content "space-between"}}
            contrainer-props)
          [:div
           [ui/app-bar
            (r/merge-props
              {:show-menu-icon-button false}
              app-bar-props)]

           [ui/selectable-list
            (r/merge-props
              {:style {:padding-top 0}
               :on-change (fn [])
               :value (str "#" (:path @active-page))}
              list-props)
            (doall
              (for [{:keys [:href :route :route-params :key] :as item-props} list-items-props]
                (let [href (cond
                             href href
                             route (d0x-ui-utils/path-for {:route route
                                                           :route-params route-params
                                                           :routes routes}))]
                  [ui/list-item
                   (r/merge-props
                     (merge {:href href
                             :value href
                             :key href})
                     (dissoc item-props :route :route-params :routes :href))])))]]]
         children)])))

(defn main-app-bar []
  (let [lg? (subscribe [:district0x/window-lg-width?])]
    (fn [props]
      [ui/app-bar
       (r/merge-props
         {:show-menu-icon-button (not @lg?)
          :on-left-icon-button-touch-tap #(dispatch [:district0x.menu-drawer/set true])}
         props)])))

(defn side-nav-menu-layout []
  (let [lg? (subscribe [:district0x/window-lg-width?])
        xs? (subscribe [:district0x/window-xs-width?])]
    (fn [drawer-menu main-app-bar & children]
      (let [{:strs [desktopGutter desktopGutterLess]} (current-component-mui-theme "spacing")
            drawer-width (current-component-mui-theme "drawer" "width")]
        [:div
         drawer-menu
         main-app-bar
         (into [:div {:style (merge {:padding-top desktopGutter
                                     :padding-bottom desktopGutter
                                     :padding-right desktopGutter
                                     :padding-left desktopGutter}
                                    (when @lg?
                                      {:padding-left (+ drawer-width desktopGutter)})
                                    (when @xs?
                                      {:padding-top desktopGutterLess
                                       :padding-bottom desktopGutterLess
                                       :padding-right desktopGutterLess
                                       :padding-left desktopGutterLess}))}]
               children)]))))






