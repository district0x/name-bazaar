(ns name-bazaar.ui.components.misc
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.ui.components.active-address-select-field :refer [active-address-select-field]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.transaction-log :refer [transaction-log gstring-transaction-name-fn on-item-click-routes-fn]]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [name-bazaar.ui.constants :as constants]))

(defn main-app-bar-right-elements []
  [row
   {:middle "xs"
    :end "xs"}
   #_(when (and (seq @my-addresses)
                @active-address-balance-dnt)
       [:h2.bolder {:style (merge styles/app-bar-balance
                                  {:margin-right 10})}
        (u/format-dnt-with-symbol @active-address-balance-dnt)])
   [transaction-log
    {:items-props {:transaction-name-fn (gstring-transaction-name-fn constants/transaction-log-tx-name-templates)
                   :on-item-click (on-item-click-routes-fn constants/transaction-log-on-item-click-routes)}}]
   [active-address-select-field
    {:select-field-props {:label-style {:color "#FFF"}}}]])

(defn side-nav-menu-layout [& children]
  (into [d0x-misc/side-nav-menu-layout
         [d0x-misc/side-nav-menu
          {}
          [[d0x-misc/nav-menu-item
            {:primary-text "Offerings"
             :key :route.offerings/search
             :route :route.offerings/search}]
           [d0x-misc/nav-menu-item
            {:primary-text "Latest"
             :key :route.offerings.search/latest
             :route :route.offerings/search
             :nested-level 1}]
           [d0x-misc/nav-menu-item
            {:primary-text "Most Active"
             :key :route.offerings.search/most-active
             :route :route.offerings/search
             :nested-level 1}]
           [d0x-misc/nav-menu-item
            {:primary-text "Ending Soon"
             :key :route.offerings.search/ending-soon
             :route :route.offerings/search
             :nested-level 1}]
           [d0x-misc/nav-menu-item
            {:primary-text "Requests"
             :key :route.offering-requests/search
             :route :route.offering-requests/search}]
           [d0x-misc/nav-menu-item
            {:primary-text "Watched Names"
             :key :route/watched-names
             :route :route/watched-names}]
           [d0x-misc/nav-menu-item
            {:primary-text "Create Offering"
             :key :route.offering/create
             :route :route.offering/create}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Offering"
             :key :route.user/my-offerings
             :route :route.user/my-offerings}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Purchases"
             :key :route.user/my-purchases
             :route :route.user/my-purchases}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Bids"
             :key :route.user/my-bids
             :route :route.user/my-bids}]
           [d0x-misc/nav-menu-item
            {:primary-text "My Settings"
             :key :route.user/my-settings
             :route :route.user/my-settings}]
           [d0x-misc/nav-menu-item
            {:primary-text "How it works"
             :key :route/how-it-works
             :route :route/how-it-works}]
           [d0x-misc/nav-menu-item
            {:primary-text "About"
             :key :route/about
             :route :route/about}]]]
         [d0x-misc/main-app-bar
          {:icon-element-right (r/as-element [main-app-bar-right-elements])}]]
        children))


