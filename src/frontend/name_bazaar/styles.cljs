(ns name-bazaar.styles
  (:require [cljs-react-material-ui.core :refer [color get-mui-theme]]))

(def theme-cyan "#7cf8fa")
(def theme-blue "#2c398f")
(def theme-green "#23fdd8")
(def theme-orange "#ffd500")
(def theme-gray "#47608e")

(def primary1-color theme-green)
(def accent1-color theme-cyan)
(def text-color "rgba(0, 0, 0, 0.87)")

(def palette {:primary1-color primary1-color
              :accent1-color accent1-color
              :text-color theme-blue})

(def mui-theme (get-mui-theme {:palette palette
                               :font-family "proxima-soft, sans-serif"
                               #_#_:paper {:background-color theme-blue
                                           :color "#FFF"}
                               :flat-button {:primary-text-color theme-blue}
                               :drop-down-menu {:accent-color theme-blue}
                               :menu-item {:selected-text-color theme-cyan}
                               :snackbar {:background-color "rgba(0, 0, 0, 0.95)"
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

;; --- GENERIC STYLES END ---