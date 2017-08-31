(ns name-bazaar.ui.styles
  (:require [cljs-react-material-ui.core :refer [get-mui-theme]]))

(def color cljs-react-material-ui.core/color)

(def primary1-color (color :cyan500))
#_(def accent1-color theme-cyan)
#_(def text-color "rgba(0, 0, 0, 0.87)")

(def palette {:primary1-color primary1-color
              #_#_:accent1-color accent1-color
              #_#_:text-color theme-blue})


(def mui-theme (get-mui-theme {:palette palette
                               :font-family "Open Sans, sans-serif"
                               :app-bar {:height 56}
                               :ripple {:color primary1-color}
                               #_#_:svg-icon {:color primary1-color}
                               #_#_:paper {:background-color theme-blue
                                           :color "#FFF"}
                               #_#_:flat-button {:primary-text-color theme-blue}
                               #_#_:drop-down-menu {:accent-color theme-blue}
                               #_#_:menu-item {:selected-text-color theme-cyan}
                               #_#_:snackbar {:background-color "rgba(0, 0, 0, 0.95)"
                                              :text-color theme-cyan}}))

;; --- GENERIC STYLES BEGNINNING ---

(def desktop-gutter (aget mui-theme "spacing" "desktopGutter"))
(def desktop-gutter-more (aget mui-theme "spacing" "desktopGutterMore"))
(def desktop-gutter-less (aget mui-theme "spacing" "desktopGutterLess"))
(def desktop-gutter-mini (aget mui-theme "spacing" "desktopGutterMini"))

(def margin-bottom-gutter
  {:margin-bottom desktop-gutter})

(def margin-bottom-gutter-more
  {:margin-bottom desktop-gutter-more})

(def margin-top-gutter-more
  {:margin-top desktop-gutter-more})

(def margin-bottom-gutter-less
  {:margin-bottom desktop-gutter-less})

(def margin-bottom-gutter-mini
  {:margin-bottom desktop-gutter-mini})

(def margin-top-gutter
  {:margin-top desktop-gutter})

(def margin-top-gutter-less
  {:margin-top desktop-gutter-less})

(def margin-top-gutter-mini
  {:margin-top desktop-gutter-mini})

(def margin-left-gutter
  {:margin-left desktop-gutter})

(def margin-left-gutter-less
  {:margin-left desktop-gutter-less})

(def margin-left-gutter-mini
  {:margin-left desktop-gutter-mini})

(def margin-right-gutter
  {:margin-right desktop-gutter})

(def margin-right-gutter-less
  {:margin-right desktop-gutter-less})

(def margin-right-gutter-mini
  {:margin-right desktop-gutter-mini})

(defn margin-all [x]
  {:margin-top x
   :margin-bottom x
   :margin-right x
   :margin-left x})

(defn margin-horizontal [x]
  {:margin-right x
   :margin-left x})

(defn margin-vertical [x]
  {:margin-top x
   :margin-bottom x})

(defn padding-all [x]
  {:padding-top x
   :padding-bottom x
   :padding-right x
   :padding-left x})

(defn padding-horizontal [x]
  {:padding-right x
   :padding-left x})

(defn padding-vertical [x]
  {:padding-top x
   :padding-bottom x})

(def text-left
  {:text-align :left})

(def text-right
  {:text-align :right})

(def text-center
  {:text-align :center})

(def full-width
  {:width "100%"})

(def full-height
  {:height "100%"})

(def no-wrap
  {:white-space :nowrap})

(def word-wrap-break
  {:word-wrap :break-word})

(def clickable
  {:cursor :pointer})

(def display-inline
  {:display :inline})

(def display-block
  {:display :block})

(def display-inline-block
  {:display :inline-block})

(def display-none
  {:display :none})

(def display-flex
  {:display :flex})

(def opacity-1
  {:opacity 1})

(def opacity-0
  {:opacity 0})

(def italic-text
  {:font-style :italic})

(def bold-text
  {:font-style :bold})

(def content-wrap
  (padding-all desktop-gutter))

(def visibility-hidden
  {:visibility :hidden})

(def visibility-visible
  {:visibility :visible})

(def text-overflow-ellipsis
  {:overflow :hidden
   :white-space :nowrap
   :text-overflow :ellipsis})

(def text-decor-none
  {:text-decoration :none})

;; --- GENERIC STYLES END ---

(def text-field-warning-color (color :orange500))
(def text-field-success-color (color :green500))

(defn search-results-list-item-height [xs?]
  (if xs? 75 52))

(defn auction-offering-list-item-expanded-height [xs?]
  (if xs? 700 430))

(defn buy-now-offering-list-item-expanded-height [xs?]
  (if xs? 400 260))

(defn search-results-list-item [xs?]
  {:padding-top desktop-gutter-mini
   :padding-bottom desktop-gutter-mini
   :padding-left (if xs? desktop-gutter-mini desktop-gutter-less)
   :padding-right (if xs? desktop-gutter-mini desktop-gutter-less)})

(def search-results-no-items
  {:text-align :center
   :width "100%"
   :font-size "1.1em"})

(defn search-results-list-item-body [xs?]
  {:padding (if xs? desktop-gutter-mini desktop-gutter-less)
   :height "100%"})

(def search-results-list-item-header
  {:transition "opacity .25s ease-in"})

(def warning-color
  {:color (color :red500)})

(def active-address-balance
  {:color "#FFF"
   :font-size 20})

(def active-address-select-field-label
  {:color "#FFF"})

(def saved-searches-select-field
  {:width "calc(100% - 48px)"})

(def offering-search-params-drawer-mobile
  {:padding desktop-gutter-less
   :overflow :hidden})

(def offerings-order-by-select-field
  {:width "calc(100% - 48px)"})

(defn placeholder-animated-background [xs?]
  {:width "100%"
   :height (- (search-results-list-item-height xs?)
              (:padding-top (search-results-list-item xs?))
              (:padding-bottom (search-results-list-item xs?)))
   :animation-duration "1s"
   :animation-fill-mode "forwards"
   :animation-iteration-count "infinite"
   :animation-name "placeHolderShimmer"
   :animation-timing-function "linear"
   :background-color "#f6f7f8"
   :background "linear-gradient(to right, #eeeeee 8%, #dddddd 18%, #eeeeee 33%)"
   :position "relative"})

(def placeholder-backgroud-masker
  {:background-color "#FFF"
   :position :absolute})

(def search-results-paper
  {:min-height 800
   :padding 0
   :padding-top desktop-gutter-less})

(defn offering-list-item-price [xs?]
  {:width "100%"
   :text-align (if xs? :left :right)
   :font-size (if xs? "1.2em" "1.3em")})

(def offering-list-item-time-left
  {:font-size "1.05em"})

(def offering-list-item-bid-count
  {:font-size "1.05em"})

(def offering-list-item-name
  {:font-size "1.3em"
   :overflow :hidden
   :text-overflow :ellipsis
   :white-space :nowrap})

(def offering-list-item-type
  {:font-size "0.75em"})

(def offering-list-item-new-owner-info
  {:font-size "1.2em"})

(def offering-list-item-bid-count-xs
  {:font-size "0.9em"})

(def text-field-suffix
  {:font-size "1.2em"})

(def page-headline
  {:margin-bottom desktop-gutter-less})

(def offering-status-chip-color
  {:offering.status/active (color :green500)
   :offering.status/finalized (color :blue500)
   :offering.status/missing-ownership (color :red700)
   :offering.status/finished (color :light-blue600)})

(def offering-detail-chip-label-color "#FFF")

(def offering-detail-chip-label
  {:font-weight :bold})

(def offering-type-chip-color
  {:buy-now-offering (color :teal600)
   :auction-offering (color :indigo300)})

(def offering-detail-center-headline
  {:font-size "1.2em"
   :margin-bottom desktop-gutter-mini})

(def offering-detail-center-value
  {:font-size "2.6em"
   :font-weight 300})

(def offering-detail-countdown-unit
  {:font-size "1.4em"
   :display :inline-block
   :text-align :left})