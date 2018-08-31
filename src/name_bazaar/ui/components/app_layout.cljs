(ns name-bazaar.ui.components.app-layout
  (:require
    [district.ui.mobile.subs :as mobile-subs]
    [district0x.ui.components.active-address-balance :refer [active-address-balance]]
    [district0x.ui.components.active-address-select :refer [active-address-select]]
    [district0x.ui.components.snackbar :refer [snackbar]]
    [district0x.ui.components.transaction-log :refer [transaction-log]]
    [district0x.ui.history :as history]
    [district0x.ui.utils :refer [truncate]]
    [name-bazaar.ui.components.app-bar-search :refer [app-bar-search]]
    [name-bazaar.ui.components.meta-tags :refer [meta-tags]]
    [name-bazaar.ui.utils :refer [offerings-newest-url offerings-most-active-url offerings-ending-soon-url path-for offerings-sold-url]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]
    [name-bazaar.ui.constants :as constants]))

(defn side-nav-menu-logo []
  [:a.side-nav-logo
   {:href (path-for :route/home)}])

(defn district0x-banner []
  [:div.district0x-banner
   [:div.logo ""]
   [:div "Part of the"]
   [:a
    {:href "https://district0x.io"
     :target :_blank}
    "district0x Network"]])

(def nav-menu-items-props [{:text "Offerings"
                            :route :route.offerings/search
                            :class :offerings-search
                            :icon "hand"}
                           {:text "Latest"
                            :href offerings-newest-url
                            :class "nested-item offerings-newest"}
                           {:text "Most Active"
                            :href offerings-most-active-url
                            :class "nested-item offerings-most-active"}
                           {:text "Ending Soon"
                            :href offerings-ending-soon-url
                            :class "nested-item offerings-ending-soon"}
                           {:text "Sold"
                            :href offerings-sold-url
                            :class "nested-item offerings-sold"}
                           {:text "Requests"
                            :route :route.offering-requests/search
                            :class :offering-requests-search
                            :icon "message-box"}
                           {:text "Watched Names"
                            :route :route/watched-names
                            :class :watched-names
                            :icon "eye"}
                           {:text "Create Offering"
                            :route :route.offerings/create
                            :class :create-offering
                            :icon "price-tag-plus"}
                           {:text "My Offerings"
                            :route :route.user/my-offerings
                            :class :my-offerings
                            :icon "price-tag"}
                           {:text "My Purchases"
                            :route :route.user/my-purchases
                            :class :my-purchases
                            :icon "bag"}
                           {:text "My Bids"
                            :route :route.user/my-bids
                            :class :my-bids
                            :icon "hammer"}
                           {:text "Manage Names"
                            :route :route.user/manage-names
                            :class :manage-names
                            :icon "user-network"}
                           {:text "My Settings"
                            :route :route.user/my-settings
                            :class :my-settings
                            :icon "settings"}
                           {:text "Instant Registration"
                            :route :route.registrar/instant-registration
                            :class :register-name
                            :icon "pencil"}
                           {:text "Register Name"
                            :route :route.registrar/register
                            :class :register-name
                            :icon "pencil"}
                           {:text "How it works"
                            :route :route/how-it-works
                            :class :how-it-works
                            :icon "book"}
                           {:text "About"
                            :route :route/about
                            :class :about
                            :icon "question"}])

(def nav-menu-items-props-no-instant-registration (remove #(= (:route %) :route.registrar/instant-registration) nav-menu-items-props))

(defn- format-user-address [address resolved-address]
  (if (= resolved-address address)
    address
    (str resolved-address " (" address ")")))

(defn address-select []
  (let [active-resolved-address (subscribe [:resolved-active-address])
        my-addresses (subscribe [:district0x/my-addresses])
        active-address (subscribe [:district0x/active-address])]
    (fn []
      [active-address-select
       {:single-address-props
        {:address (format-user-address @active-address @active-resolved-address)}
        :select-field-props
        {:options (doall
                    (for [address @my-addresses]
                      {:value address
                       :text (format-user-address address @(subscribe [:reverse-resolved-address address]))}))}}])))


(defn mobile-coinbase-overlay []
  (let [mobile-coinbase-appstore-link @(subscribe [::mobile-subs/coinbase-appstore-link])]
    (fn []
      [:div#mobile-coinbase-overlay
       {:on-click #(dispatch [:district0x.location/nav-to-url mobile-coinbase-appstore-link])}
       [:span "Make offers with"]
       [:img {:src "/images/coinbase_logo.png"}]])))


(defn app-bar []
  (let [open? (subscribe [:district0x.transaction-log/open?])
        my-addresses (subscribe [:district0x/my-addresses])
        mobile-coinbase-compatible? @(subscribe [::mobile-subs/coinbase-compatible?])]
    
    (fn []
      [:div.app-bar
       [:div.left-section
        [address-select]
        [:i.icon.hamburger
         {:on-click (fn [e]
                      (dispatch [:district0x.menu-drawer/set true])
                      (.stopPropagation e))}]]
       [:div.middle-section
        [app-bar-search]]
       [:div.right-section
        {:on-click (fn []
                     (if (empty? @my-addresses)
                       (dispatch [:district0x.location/nav-to :route/how-it-works {} constants/routes])
                       (dispatch [:district0x.transaction-log/set-open (not @open?)])))}
        (if (empty? @my-addresses)
          [:div "No Accounts"]
          [active-address-balance])
        [:i.icon.transactions]]
       (when (and (empty? @my-addresses) mobile-coinbase-compatible?)
         [mobile-coinbase-overlay])])))

(defn app-layout []
  (let [drawer-open? (subscribe [:district0x/menu-drawer-open?])
        min-computer-screen? (subscribe [:district0x.window.size/min-computer-screen?])
        active-page (subscribe [:district0x/active-page])
        app-container-ref (r/atom nil)
        use-instant-registrar? (subscribe [:district0x/config :use-instant-registrar?])]
    (fn [{:keys [:meta]} & children]
      [:div.app-container
       {:ref (fn [el]
               (when (and el (not @app-container-ref))
                 (reset! app-container-ref el)))}
       [meta-tags meta]
       [ui/Sidebar
           {:as (aget js/semanticUIReact "Menu")
            :visible (or @drawer-open? @min-computer-screen?)
            :animation "overlay"
            :vertical true
            :inverted true
            :fixed :left}
           [:div.menu-content
            {:style {:overflow-y :scroll}}
            [side-nav-menu-logo]
            (doall
              (for [{:keys [:text :route :href :class :icon :on-click]} (if @use-instant-registrar?
                                                                          nav-menu-items-props
                                                                          nav-menu-items-props-no-instant-registration)]
                (let [href (or href (path-for route))]
                  [ui/MenuItem
                   {:key text
                    :as "a"
                    :href href
                    :class class
                    :on-click #(dispatch [:district0x.window/scroll-to-top])
                    :active (= (str (when history/hashroutes? "#") (:path @active-page)) href)}
                   [:i.icon
                    {:class icon}]
                   text])))
            [district0x-banner]]]
       [:div.app-content
        {:on-click (fn []
                     (when-not @min-computer-screen?
                       (dispatch [:district0x.menu-drawer/set false])))}
        [app-bar]
        [ui/Sticky
         {:class :transaction-log-sticky
          :context @app-container-ref}
         [transaction-log
          {:title-props {:on-click #(dispatch [:district0x.transaction-log/set-open false])}}]]
        [:div.grid.main-content
         children]]
       [snackbar
        {:action-button-props {:primary true
                               :size :small}}]])))
