(ns name-bazaar.ui.components.date-picker
  (:require
    [cljsjs.react-datepicker]
    [reagent.core :as r]))

(def date-picker* (r/adapt-react-class (aget js/DatePicker "default")))

(defn date-picker [{:keys [:selected] :as props}]
  [date-picker*
   (r/merge-props
     props
     {:selected (js/moment selected)})])

