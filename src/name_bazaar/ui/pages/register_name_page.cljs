(ns name-bazaar.ui.pages.register-name-page
  (:require
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [district0x.ui.components.text-field :refer [text-field-with-suffix]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [valid-ens-name?]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn register-name-form []
  (let [xs? (subscribe [:district0x/window-xs-width?])
        label (r/atom "")]
    (fn []
      [:div
       {:style {:height (when-not @xs? 250)
                :display :flex
                :flex-direction :column}}
       [:div
        [text-field-with-suffix
         {:floating-label-text "Name"
          :full-width @xs?
          :value @label
          :on-change (fn [_ value]
                       (when (valid-ens-name? value)
                         (reset! label value)))}
         [:span
          {:style styles/text-field-suffix}
          constants/registrar-root]]]
       [row
        {:bottom "xs"
         :end "xs"
         :style styles/full-height}
        [raised-transaction-button
         {:label "Register"
          :primary true
          :disabled (empty? @label)
          :full-width @xs?
          :on-click (fn []
                      (when (and (not (empty? @label))
                                 (valid-ens-name? @label))
                        (dispatch [:registrar/register {:ens.record/label @label}])
                        (reset! label "")))}]]])))

(defmethod page :route.mock-registrar/register []
  [side-nav-menu-center-layout
   [paper
    [:h1 "Register Name"]
    [register-name-form]]])
