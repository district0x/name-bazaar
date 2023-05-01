(ns name-bazaar.ui.components.date-picker
  (:require
    ["react-datepicker" :as DatePicker]
    [reagent.core :as r]
    ["moment" :as moment]))

(def date-picker* (r/adapt-react-class (.-default DatePicker)))

(defn date-picker [{:keys [:selected] :as props}]
  [date-picker*
   (r/merge-props
     props
     {:selected (.toDate (moment selected))
      :previousMonthButtonLabel nil
      :nextMonthButtonLabel nil})])
