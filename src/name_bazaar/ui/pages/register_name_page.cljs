(ns name-bazaar.ui.pages.register-name-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.ens-record.ens-name-input :refer [ens-name-input]]
    [name-bazaar.ui.utils :refer [valid-ens-name? path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn register-name-form []
  (let [label (r/atom "")]
    (fn []
      [ui/Grid
       {:class "layout-grid submit-footer"}
       [ui/GridRow
        [ui/GridColumn
         {:mobile 16
          :tablet 8
          :computer 6}
         [ens-name-input
          {:label "Name"
           :fluid true
           :value @label
           :on-change (fn [_ data]
                        (let [value (aget data "value")]
                          (when (valid-ens-name? value)
                            (reset! label value))))}]]]
       [ui/GridRow
        {:centered true}
        [:div
         [transaction-button
          {:primary true
           :disabled (empty? @label)
           :pending-text "Registering..."
           :on-click (fn []
                       (when (and (not (empty? @label))
                                  (valid-ens-name? @label))
                         (dispatch [:registrar/register {:ens.record/label @label}])
                         (reset! label "")))}
          "Register"]]]])))

(defmethod page :route.registrar/register []
  [app-layout {:meta {:title "NameBazaar - Register ENS Name"
                      :description "Simplest way to register a new ENS name."}}
   [ui/Segment
    [:h1.ui.header.padded "Register Name"]
    [register-name-form]]])
