(ns name-bazaar.ui.pages.manage-names-page
  (:require
    [cljs-time.coerce :as time-coerce :refer [to-epoch to-date from-date]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [pos-ether-value?]]
    [district0x.ui.components.input :refer [input token-input]]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :refer [date+time->local-date-time format-time-duration-units]]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.date-picker :refer [date-picker]]
    [name-bazaar.ui.components.ens-record.ens-name-input :refer [ens-name-input]]
    [name-bazaar.ui.components.loading-placeholders :refer [content-placeholder]]
    [name-bazaar.ui.components.offering.offering-type-select :refer [offering-type-select]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [namehash sha3 normalize strip-root-registrar-suffix valid-ens-name? path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(def ownership-status->text
  {:ens.ownership-status/empty-name ""
   :ens.ownership-status/loading "Checking ownership..."
   :ens.ownership-status/not-ens-record-owner "You are not owner of this name"
   :ens.ownership-status/not-deed-owner "You don't own this name's locked value"
   :ens.ownership-status/owner "You are owner of this name"})

(defn load-name-ownership [value]
  (let [full-name (str value constants/registrar-root)
        node (namehash full-name)]
    (dispatch [:ens.records/load [node]])
    (when (top-level-name? full-name)
      (dispatch [:registrar.entries/load [(sha3 value)]]))))

(defn ens-name-text-field [{:keys [:value]}]
  (fn [{:keys [:on-change :value] :as props}]
    (let [ownership-status @(subscribe [:ens.record/ownership-status (when (seq value)
                                                                       (str value constants/registrar-root))])
          error? (contains? #{:ens.ownership-status/not-ens-record-owner
                              :ens.ownership-status/not-deed-owner}
                            ownership-status)]
      [:div.input-state-label
       {:class (cond
                 error? :error
                 (contains? #{:ens.ownership-status/owner} ownership-status) :success)}
       [ens-name-input
        (r/merge-props
          {:label "Name"
           :fluid true
           :value value
           :error error?
           :on-change (fn [e data]
                        (let [value (aget data "value")]
                          (when (valid-ens-name? value)
                            (let [value (normalize value)]
                              (aset data "value" value)
                              (on-change e data)
                              (load-name-ownership value)))))}
          (dissoc props :ownership-status :on-change))]
       [:div.ui.label (ownership-status->text ownership-status)]])))

(defn point-name-form []
  (let [form-data (r/atom {})]
    (fn [{:keys [:editing?]}]
      (let [{:keys [:name-manager/address :name-manager/name]} @form-data
            ownership-status (when-not editing?
                               @(subscribe [:ens.record/ownership-status (when (seq name)
                                                                           (str name constants/registrar-root))]))
            submit-disabled? (or (and (not editing?)
                                      (not= ownership-status :ens.ownership-status/owner)))]
        [ui/Grid
         {:class "layout-grid submit-footer offering-form"
          :celled "internally"}
         [ui/GridRow
          [ui/GridColumn
           {:width 16}
           [ui/Grid
            {:relaxed "very"}
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [ens-name-text-field
              {:value name
               :disabled editing?
               :on-change #(swap! form-data assoc :name-manager/name (aget %2 "value"))}]]]]]
         [ui/GridRow
          {:centered true}
          [:div
           (if-not editing?
             [transaction-button
              {:primary true
               :disabled submit-disabled?
               ;; :pending? @(subscribe [:name-manager/tx-pending? address])
               :pending-text "Saving Changes..."
               :on-click (fn []
                           (dispatch [(dispatch [:name-manager/point-name @form-data])]))}
              "Setup resolver"])]]]))))

(defmethod page :route.user/manage-names []
  (let [query-params (subscribe [:district0x/query-params])]
    (fn []
      (let [{:keys [:name]} @query-params]
        [app-layout {:meta {:title "NameBazaar - Manage Names" :description "Manage your ENS names"}}
         [ui/Segment
          [:h1.ui.header.padded "Point Name to an Address"]
          [point-name-form
           {:default-name (when (and name (valid-ens-name? name))
                            (strip-root-registrar-suffix (normalize name)))}]]]))))
