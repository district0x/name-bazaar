(ns name-bazaar.ui.pages.offerings-search-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.components.misc :refer [side-nav-menu-layout]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn transaction-log []
  (let [settings (subscribe [:district0x/transaction-log-settings])
        tx-log (subscribe [:district0x/transaction-log])]
    (fn []
      (print.foo/look @tx-log)
      (let [{:keys [:from-active-address-only?]} @settings]
        [ui/menu
         {:style {:width 320
                  :border "1px solid #555"
                  :padding-left 0}
          :list-style {:padding-top 2
                       :padding-left 0}
          :max-height 400}
         [:div
          {:style {:position :relative
                   :width "100%"
                   :padding-top 5}}
          [:div {:style {:text-align "center"
                         :font-size 12
                         :font-weight "bold"}}
           "TRANSACTION LOG"]]
         [:div
          {:style {:width "100%"
                   :margin-top 10
                   :padding-left 5}}
          [ui/toggle
           {:label "Show transactions from active address only"
            :label-position "right"
            :label-style {:font-size 12
                          :max-width 300}
            :on-toggle #(dispatch [:district0x.transaction-log-settings/set :from-active-address-only? %2])
            :toggled from-active-address-only?}]]
         [:div
          {:style {:margin-top 20}}
          (if (seq @tx-log)
            (for [{:keys [:hash]} @tx-log]
              [ui/menu-item
               {:key hash
                :style {:border-bottom "0.5px solid #eee"}
                :inner-div-style {:padding 10}
                :on-touch-tap (fn [e]
                                (when-not (instance? js/HTMLAnchorElement (aget e "target"))
                                  (println "menu item touch tap")))}
               [:div
                [:div {:style {:line-height "25px"}} "Request name etherscan.eth"]
                (let [line-styles {:line-height "14px" :font-size "11.5px" :color "#333"}]
                  [row-with-cols
                   [col
                    {:xs 8}
                    [:div {:style line-styles} "Sent 52 min. ago"]
                    [:div {:style line-styles} "Gas used: 45040 ($0.09)"]
                    [:div {:style line-styles} "From: " [:a {:href "https://github.com"
                                                             :target :_blank
                                                             :style {:text-decoration :underline}}
                                                         "0x14c4c962701f4..."]]
                    [:div {:style line-styles} "Tx ID: " [:a {:href "https://github.com"
                                                              :target :_blank
                                                              :style {:text-decoration :underline}}
                                                          "0x14c4c962701f4..."]]]
                   [col
                    {:xs 4
                     :style {:text-align :right}}
                    [row
                     {:end "xs"
                      :bottom "xs"
                      :style {:height "100%"}}
                     [:div
                      [:div {:style {:line-height "14px" :font-size "11.5px"}} "Completed"]
                      [:div {:style {:line-height "22px" :font-size "18px"}} "25.46 ETH"]]]
                    ]])]])
            [:div "No transactions"])]]))))

(defmethod page :route.offerings/search []
  (fn []
    [side-nav-menu-layout
     [center-layout
      [paper
       #_ [transaction-log]
       #_[ui/menu-item {:primary-text "asda"}]]]]))
