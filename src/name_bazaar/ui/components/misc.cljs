(ns name-bazaar.ui.components.misc
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [components.active-address-balance :refer [active-address-balance]]
    [district0x.ui.components.active-address-select-field :refer [active-address-select-field]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.transaction-log :refer [transaction-log]]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.components.app-bar-search :refer [app-bar-search]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [offerings-newest-url offerings-most-active-url offerings-ending-soon-url]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn a [props body]
  [d0x-misc/a
   (assoc props :routes constants/routes)
   body])

(defn main-app-bar-right-elements []
  (let [xs-sm? (subscribe [:district0x/window-xs-sm-width?])
        xs? (subscribe [:district0x/window-xs-width?])
        active-page (subscribe [:district0x/active-page])]
    (fn []
      [row
       {:middle "xs"
        :end "xs"}
       (when-not @xs?
         [app-bar-search
          {:style styles/margin-right-gutter-less}])
       (when-not @xs-sm?
         [active-address-balance
          {:style styles/active-address-balance}])
       (when (and @xs-sm? (= (:handler @active-page) :route.offerings/search))
         [ui/icon-button
          {:on-click #(dispatch [:offerings.search-params-drawer/set true])}
          (icons/filter {:color "#FFF"})])
       [transaction-log
        {:icon-menu-props {:style {:margin-right styles/desktop-gutter-mini}}}]
       (when-not @xs-sm?
         [active-address-select-field
          {:select-field-props {:label-style styles/active-address-select-field-label}}])])))

(defn side-nav-menu-logo []
  [a
   {:route :route/home}
   [:img
    {:src "./images/logo@2x.png"
     :style styles/side-nav-menu-logo}]])

(defn district0x-banner []
  [row
   {:start "xs"
    :style (merge styles/full-width
                  styles/margin-bottom-gutter-mini
                  styles/margin-left-gutter-less)}
   [:div
    {:style styles/district0x-banner-text}
    "Part of district0x Network"]
   [:a
    {:href "https://district0x.io"
     :target :_blank}
    [:img {:alt "district0x"
           :src "./images/district0x-logo-small.png"
           :style styles/district0x-banner-logo}]]])

(def nav-menu-items-props
  (->> [{:primary-text "Offerings"
         :route :route.offerings/search}
        {:primary-text "Latest"
         :href offerings-newest-url
         :nested-level 1
         :style styles/nav-menu-item-nested}
        {:primary-text "Most Active"
         :href offerings-most-active-url
         :nested-level 1
         :style styles/nav-menu-item-nested}
        {:primary-text "Ending Soon"
         :href offerings-ending-soon-url
         :nested-level 1
         :style styles/nav-menu-item-nested}
        {:primary-text "Requests"
         :route :route.offering-requests/search}
        {:primary-text "Watched Names"
         :route :route/watched-names}
        {:primary-text "Create Offering"
         :route :route.offerings/create}
        {:primary-text "My Offerings"
         :route :route.user/my-offerings}
        {:primary-text "My Purchases"
         :route :route.user/my-purchases}
        {:primary-text "My Bids"
         :route :route.user/my-bids}
        {:primary-text "My Settings"
         :route :route.user/my-settings}
        {:primary-text "Register Name"
         :route :route.mock-registrar/register}
        {:primary-text "How it works"
         :route :route/how-it-works}
        {:primary-text "About"
         :route :route/about}]
    (map (fn [props]
           (r/merge-props
             {:style styles/nav-menu-item
              :inner-div-style styles/nav-menu-item-inner-div
              :disable-touch-ripple true}
             props)))))

(defn side-nav-menu-layout [& children]
  (into [d0x-misc/side-nav-menu-layout
         [d0x-misc/side-nav-menu
          {:app-bar-props
           {:style styles/side-nav-menu-app-bar
            :show-menu-icon-button true
            :icon-style-left styles/side-nav-menu-logo-wrap
            :icon-element-left (r/as-element [side-nav-menu-logo])}
           :list-items-props nav-menu-items-props
           :routes constants/routes}
          [district0x-banner]]
         [d0x-misc/main-app-bar
          {:icon-element-right (r/as-element [main-app-bar-right-elements])}]]
        children))

(defn side-nav-menu-center-layout [& children]
  [side-nav-menu-layout
   (into [center-layout] children)])


