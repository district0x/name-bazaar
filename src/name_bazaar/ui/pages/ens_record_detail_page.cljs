(ns name-bazaar.ui.pages.ens-record-detail-page
  (:require
    [district0x.ui.components.misc :refer [page]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.ens-name-details :refer [ens-name-details]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.offerings-order-by-select :refer [offerings-order-by-select]]
    [name-bazaar.ui.utils :refer [name->label-hash registrar-entry-state->text]]
    [name-bazaar.shared.utils :refer [name-label top-level-name?]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn ens-record-offerings-order-by-select-field []
  (let [search-results (subscribe [:offerings/ens-record-offerings])]
    (fn []
      (let [{:keys [:params]} @search-results]
        [offerings-order-by-select
         {:order-by-column (first (:order-by-columns params))
          :order-by-dir (first (:order-by-dirs params))
          :fluid true
          :options [:offering.order-by/newest
                    :offering.order-by/most-expensive
                    :offering.order-by/cheapest]
          :on-change (fn [order-by-column order-by-dir]
                       (dispatch [:offerings.ens-record-offerings/set-params-and-search
                                  {:order-by-columns [order-by-column]
                                   :order-by-dirs [order-by-dir]}]))}]))))

(defn ens-record-offerings []
  (let [route-params (subscribe [:district0x/route-params])
        search-results (subscribe [:offerings/ens-record-offerings])]
    (fn []
      (let [{:keys [:ens.record/name]} @route-params
            {:keys [:items :loading? :params :total-count]} @search-results]
        [ui/Segment
         [:h1.ui.header.padded name " Offerings"]
         [ui/Grid
          {:padded true
           :class "no-inner-horizontal-padding mobile-inner-vertical-padding join-lower"}
          [ui/GridColumn
           {:computer 6
            :tablet 8
            :mobile 16
            :floated :right}
           [ens-record-offerings-order-by-select-field]]]
         [offering-infinite-list
          {:class "secondary"
           :total-count total-count
           :offset (:offset params)
           :loading? loading?
           :no-items-text "No offerings were created for this name yet"
           :on-next-load (fn [offset limit]
                           (dispatch [:offerings.ens-record-offerings/set-params-and-search
                                      {:offset offset :limit limit} {:append? true}]))}
          (doall
            (for [[i offering] (medley/indexed items)]
              [offering-list-item
               {:key i
                :offering offering
                :header-props {:show-created-on? true
                               :show-sold? true
                               :show-active? true}}]))]]))))

(defmethod page :route.ens-record/detail []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      (let [{:keys [:ens.record/name]} @route-params
            {:keys [:registrar.entry/state :registrar.entry.deed/address
                    :registrar.entry/registration-date :registrar.entry.deed/value
                    :registrar.entry.deed/address]}
            @(subscribe [:registrar/entry (name->label-hash name)])
            state-text (registrar-entry-state->text (if (and (top-level-name? name)
                                                         (< (count (name-label name)) 7))
                                                  :registrar.entry.state/not-yet-available
                                                  state))]
        [app-layout {:meta {:title (str name " name details")
                            :description (str "See details about " name ". Status: " state-text)}}
         [ui/Segment
          [:h1.ui.header.padded name]
          [ens-name-details
           {:ens.record/name name
            :registrar.entry/state-text state-text}]]
         [ens-record-offerings]]))))
