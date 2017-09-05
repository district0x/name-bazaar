(ns name-bazaar.ui.components.misc
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [components.active-address-balance :refer [active-address-balance]]
    [district0x.ui.components.active-address-select-field :refer [active-address-select-field]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.transaction-log :refer [transaction-log]]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn a [props body]
  [d0x-misc/a
   (assoc props :routes constants/routes)
   body])

(defn main-app-bar-right-elements []
  (let [xs-sm? (subscribe [:district0x/window-xs-sm-width?])
        active-page (subscribe [:district0x/active-page])]
    (fn []
      [row
       {:middle "xs"
        :end "xs"}
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

(defn side-nav-menu-layout [& children]
  (into [d0x-misc/side-nav-menu-layout
         [d0x-misc/side-nav-menu
          {}
          [[d0x-misc/nav-menu-item
            {:primary-text "Offerings"
             :route :route.offerings/search
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "Latest"
             :key :route.offerings.search/latest
             :route :route.offerings/search
             :routes constants/routes
             :nested-level 1}]
           [d0x-misc/nav-menu-item
            {:primary-text "Most Active"
             :key :route.offerings.search/most-active
             :route :route.offerings/search
             :routes constants/routes
             :nested-level 1}]
           [d0x-misc/nav-menu-item
            {:primary-text "Ending Soon"
             :key :route.offerings.search/ending-soon
             :route :route.offerings/search
             :routes constants/routes
             :nested-level 1}]
           [d0x-misc/nav-menu-item
            {:primary-text "Requests"
             :route :route.offering-requests/search
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "Watched Names"
             :route :route/watched-names
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "Create Offering"
             :route :route.offerings/create
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Offerings"
             :route :route.user/my-offerings
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Purchases"
             :route :route.user/my-purchases
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Bids"
             :route :route.user/my-bids
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Settings"
             :route :route.user/my-settings
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "Register Name"
             :route :route.mock-registrar/register
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "How it works"
             :route :route/how-it-works
             :routes constants/routes}]
           [d0x-misc/nav-menu-item
            {:primary-text "About"
             :route :route/about
             :routes constants/routes}]]]
         [d0x-misc/main-app-bar
          {:icon-element-right (r/as-element [main-app-bar-right-elements])}]]
        children))

(defn side-nav-menu-center-layout [& children]
  [side-nav-menu-layout
   (into [center-layout] children)])


