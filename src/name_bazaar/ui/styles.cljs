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

;; --- GENERIC STYLES END ---

(def search-results-list-item-height 52)
(def auction-offering-list-item-expanded-height 410)
(def buy-now-offering-list-item-expanded-height 260)

(def search-results-list-item
  {:padding-top desktop-gutter-mini
   :padding-bottom desktop-gutter-mini
   :padding-left desktop-gutter-less
   :padding-right desktop-gutter-less})

(def search-results-list-item-body
  {:padding desktop-gutter-less
   :height "100%"})

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

(def placeholder-animated-background
  {:width "100%"
   :height (- search-results-list-item-height
              (search-results-list-item :padding-top)
              (search-results-list-item :padding-bottom))
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
  {:min-height 600})

(def search-results-paper-inner
  {:padding-bottom 0
   :padding-left 0
   :padding-right 0})



