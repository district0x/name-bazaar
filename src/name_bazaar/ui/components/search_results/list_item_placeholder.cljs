(ns name-bazaar.ui.components.search-results.list-item-placeholder
  (:require
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn placeholder-masker [{:keys [:height :top :bottom :left-offset-perc :left-offset-perc-min :left-offset-perc-max
                                  full-width]
                           :as props}]
  (let [left-offset-perc (if (and left-offset-perc-min left-offset-perc-max)
                           (+ left-offset-perc-min (rand-int (- left-offset-perc-max left-offset-perc-min)))
                           left-offset-perc)]
    (fn []
      [:div
       (r/merge-props
         {:style (merge styles/placeholder-backgroud-masker
                        {:height height
                         :top top
                         :bottom bottom}
                        (when full-width
                          {:width "100%"})
                        (when left-offset-perc
                          {:width (str (- 100 left-offset-perc) "%") :left (str left-offset-perc "%")}))}
         (dissoc props :height :top :bottom :left-offset-perc :left-offset-perc-min :left-offset-perc-max :full-width))])))

(defn list-item-placeholder []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [props]
      (let [xs? (if (:xs? props) true @xs?)]
        [:div
         (r/merge-props
           {:style (styles/placeholder-animated-background xs?)}
           (dissoc props :xs?))
         [placeholder-masker {:height 3 :top 0 :full-width true}]
         [placeholder-masker {:height 13 :left-offset-perc 12}]
         [placeholder-masker {:height 7 :top 12 :full-width true}]
         [placeholder-masker {:height 20 :top 18 :left-offset-perc-min 20 :left-offset-perc-max 40}]
         (when xs?
           [placeholder-masker {:height 5 :top 37 :full-width true}])
         (when xs?
           [placeholder-masker {:height 20 :top 40 :left-offset-perc-min 30 :left-offset-perc-max 50}])]))))