(ns name-bazaar.ui.pages.offering-detail-page
  (:require
    [cljs-time.coerce :as time-coerce :refer [to-epoch to-date]]
    [cljs-time.core :as t]
    [district0x.ui.components.misc :as misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :refer [to-locale-string time-unit->text pluralize]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.loading-placeholders :refer [content-placeholder]]
    [name-bazaar.ui.components.offering.bottom-section :refer [offering-bottom-section]]
    [name-bazaar.ui.components.offering.general-info :refer [offering-general-info]]
    [name-bazaar.ui.components.offering.infinite-list :refer [offering-infinite-list]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.middle-section :refer [offering-middle-section]]
    [name-bazaar.ui.components.share-buttons :refer [share-buttons]]
    [name-bazaar.ui.components.offering.tags :refer [offering-type-tag offering-status-tag]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(def offering-status->text
  {:offering.status/emergency "Emergency Cancel"
   :offering.status/active "Active"
   :offering.status/finalized "Completed"
   :offering.status/missing-ownership "Missing Ownership"
   :offering.status/auction-ended "Auction Ended"})

(defn offering-detail-status-tag []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn [props]
      (let [status @(subscribe [:offering/status (:offering/address @route-params)])]
        [offering-status-tag
         {:offering/status status}]))))

(defn offering-detail-type-tag []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn [props]
      (let [{:keys [:offering/type]} @offering]
        [offering-type-tag
         {:offering/type type}]))))

(defn auction-offering-countdown []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn [props]
      (let [{:keys [:offering/address]} @offering
            time-remaining @(subscribe [:auction-offering/end-time-countdown address])]
        [ui/Grid
         {:columns "equal"
          :divided true
          :text-align :center
          :vertical-align :middle}
         (for [unit [:days :hours :minutes :seconds]]
           (let [amount (get time-remaining unit 0)]
             [ui/GridColumn
              {:key unit}
              [:div.stat-number amount]
              [:div.time-unit (pluralize (time-unit->text unit) amount)]]))]))))

(defn offering-stats [{:keys [:offering]}]
  (let [offering (subscribe [:offerings/route-offering])]
    (fn []
      (let [{:keys [:offering/price :offering/type :offering/auction? :auction-offering/bid-count]} @offering
            price-formatted (str (to-locale-string price 4) " ETH")]
        [:div.offering-stats
         {:class type}
         (if auction?
           [ui/Grid
            {:celled true
             :columns 3
             :centered true}
            [ui/GridColumn
             {:width 8
              :class :price}
             [:i.icon.dollar-circle]
             [:div.offering-stat
              [:h5.ui.header.sub
               (if (pos? bid-count) "Highest Bid" "Starting Price")]
              [:div.stat-number price-formatted]]]
            [ui/GridColumn
             {:width 8
              :class :bid-count}
             [:i.icon.hammer]
             [:div.offering-stat
              [:h5.ui.header.sub "Number of bids"]
              [:div.stat-number bid-count]]]
            [ui/GridRow
             [ui/GridColumn
              {:width 16
               :class :time-remaining}
              [:i.icon.clock]
              [:div.offering-stat
               [:h5.ui.header.sub "Time Remaining"]
               [auction-offering-countdown]]]]]
           [ui/Grid
            {:columns 1
             :celled true}
            [ui/GridColumn
             {:class :price}
             [:i.icon.dollar-circle]
             [:div.offering-stat
              [:h5.ui.header.sub "Price"]
              [:div.stat-number price-formatted]]]])]))))

(defn offering-detail []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn []
      [ui/Grid
       {:class "layout-grid submit-footer offering-detail"
        :celled "internally"}
       [ui/GridRow
        [ui/GridColumn
         {:mobile 16
          :computer 8}
         [:div.tags
          [offering-detail-status-tag]
          [offering-detail-type-tag]]
         [:div
          [offering-general-info
           {:offering @offering}]]]
        [ui/GridColumn
         {:mobile 16
          :computer 8}
         [offering-stats]]]
       [ui/GridRow
        {:centered true}
        [offering-middle-section
         {:offering @offering}]]
       [ui/GridRow
        {:centered true}
        [offering-bottom-section
         {:offering @offering}]]])))

(defn similar-offerings []
  (let [search-results (subscribe [:offerings/similar-offerings])]
    (fn []
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [ui/Segment
         [:h1.ui.header.padded "Similar Offerings"]
         [offering-infinite-list
          {:class "secondary"
           :total-count total-count
           :offset (:offset params)
           :loading? loading?
           :no-items-text "No similar offerings are currently created."
           :on-next-load (fn [offset limit]
                           (dispatch [:offerings.similar-offerings/set-params-and-search
                                      {:offset offset :limit limit} {:append? true}]))}
          (doall
            (for [[i offering] (medley/indexed items)]
              [offering-list-item
               {:key i
                :offering offering}]))]]))))

(defmethod page :route.offerings/detail []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      (let [{:keys [:offering/address]} @route-params
            offering-loaded? @(subscribe [:offering/loaded? address])
            offering @(subscribe [:offering address])]
        [app-layout
         [ui/Segment
          [ui/Grid
           {:class "layout-grid"}
           [ui/GridRow
            [ui/GridColumn
             {:class :join-lower
              :computer 8
              :tablet 8
              :mobile 16}
             [:h1.ui.header "Offering " (:offering/name offering)]]
            [ui/GridColumn
             {:class :join-lower
              :computer 8
              :tablet 8
              :mobile 16
              :floated "right"}
             [share-buttons]]]]
          (if offering-loaded?
            [offering-detail]
            [:div.padded [content-placeholder]])]
         [similar-offerings]]))))
