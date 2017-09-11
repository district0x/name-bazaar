(ns name-bazaar.ui.pages.offering-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljs-time.coerce :as time-coerce :refer [to-epoch to-date]]
    [cljs-time.core :as t]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [to-locale-string time-unit->text]]
    [name-bazaar.shared.utils :refer [name-level]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.components.offering.action-form :refer [action-form]]
    [name-bazaar.ui.components.offering.general-info :refer [offering-general-info]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.offering.warnings :refer [non-ascii-characters-warning missing-ownership-warning sub-level-name-warning]]
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [namehash sha3 strip-eth-suffix offering-type->text]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [medley.core :as medley]))

(def offering-status->text
  {:offering.status/emergency "Emergency Cancel"
   :offering.status/active "Active"
   :offering.status/finalized "Completed"
   :offering.status/missing-ownership "Missing Ownership"
   :offering.status/auction-ended "Finished"})

(defn offering-status-chip []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn [props]
      (let [status @(subscribe [:offering/status (:offering/address @route-params)])]
        [ui/chip
         (r/merge-props
           {:background-color (styles/offering-status-chip-color status)
            :label-color styles/offering-detail-chip-label-color
            :label-style styles/offering-detail-chip-label}
           props)
         (offering-status->text status)]))))

(defn offering-type-chip []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn [props]
      (let [{:keys [:offering/auction? :offering/type]} @offering]
        [ui/chip
         (r/merge-props
           {:background-color (if auction?
                                styles/offering-auction-chip-color
                                styles/offering-buy-now-chip-color)
            :label-color styles/offering-detail-chip-label-color
            :label-style styles/offering-detail-chip-label}
           props)
         (offering-type->text type)]))))

(defn offering-price-row []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn [props]
      (let [{:keys [:offering/buy-now? :offering/price :auction-offering/bid-count]} @offering]
        [row-with-cols
         (r/merge-props
           {:center "xs"}
           props)
         [col
          {:xs 12
           :style styles/offering-detail-center-headline}
          (cond
            buy-now? "price"
            (pos? bid-count) "higest bid"
            :else "starting price")]
         [col
          {:xs 12
           :style styles/offering-detail-center-value}
          (to-locale-string price 3) " ETH"]]))))

(defn end-time-countdown-unit [{:keys [:amount :unit]}]
  [:span
   [:span
    {:style styles/offering-detail-center-value}
    " "
    (when (and (not= unit :days) (< amount 10))
      0)
    amount " "]
   [:span
    {:style (merge styles/offering-detail-countdown-unit)}
    (d0x-ui-utils/pluralize (time-unit->text unit) amount)]])

(defn auction-offering-end-time-countdown-row []
  (let [xs? (subscribe [:district0x/window-xs-width?])
        offering (subscribe [:offerings/route-offering])]
    (fn [props]
      (let [{:keys [:offering/address :offering/auction?]} @offering
            {:keys [:days :hours :minutes :seconds] :as time-remaining}
            @(subscribe [:auction-offering/end-time-countdown address])]
        (when auction?
          [row-with-cols
           (r/merge-props
             {:center "xs"}
             props)
           [col
            {:xs 12
             :style styles/offering-detail-center-headline}
            "time remaining"]
           [col
            {:xs 12}
            (when (pos? days)
              [end-time-countdown-unit
               {:unit :days
                :amount days}])
            (when (or (pos? days) (pos? hours))
              [end-time-countdown-unit
               {:unit :hours
                :amount hours}])
            (when @xs? [:br])
            (when (or (pos? days) (pos? hours) (pos? minutes))
              [end-time-countdown-unit
               {:unit :minutes
                :amount minutes}])
            (when (or (pos? days) (pos? hours) (pos? minutes) (pos? seconds))
              [end-time-countdown-unit
               {:unit :seconds
                :amount seconds}])
            (when (every? zero? (vals time-remaining))
              [:span
               {:style styles/offering-detail-center-value}
               "finished"])]])))))

(defn offering-bid-count-row []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn [props]
      (let [{:keys [:offering/auction? :auction-offering/bid-count]} @offering]
        (when auction?
          [row-with-cols
           (r/merge-props
             {:center "xs"}
             props)
           [col
            {:xs 12
             :style styles/offering-detail-center-headline}
            "number of bids"]
           [col
            {:xs 12
             :style styles/offering-detail-center-value}
            bid-count]])))))

(defn warnings [{:keys [:offering] :as props}]
  (let [{:keys [:offering/address :offering/contains-non-ascii? :offering/top-level-name? :offering/original-owner]} offering]
    [:div
     (r/merge-props
       {:style styles/full-width}
       (dissoc props :offering))
     (when @(subscribe [:offering/show-missing-ownership-warning? address])
       [missing-ownership-warning
        {:offering/original-owner original-owner}])
     (when (not top-level-name?)
       [sub-level-name-warning
        {:offering/name name
         :style styles/margin-top-gutter-mini}])
     (when contains-non-ascii?
       [non-ascii-characters-warning
        {:style styles/margin-top-gutter-mini}])]))

(defn offering-detail []
  (let [offering (subscribe [:offerings/route-offering])]
    (fn []
      [:div
       [row
        [offering-status-chip]
        [offering-type-chip
         {:style styles/margin-left-gutter-mini}]]
       [row
        [offering-general-info
         {:offering @offering
          :style (merge styles/margin-top-gutter-less
                        styles/text-overflow-ellipsis)}]]
       [warnings
        {:offering @offering
         :style styles/margin-top-gutter-less}]
       [offering-price-row
        {:style styles/margin-top-gutter-more}]
       [offering-bid-count-row
        {:style styles/margin-top-gutter-less}]
       [auction-offering-end-time-countdown-row
        {:style styles/margin-top-gutter-less}]
       [row
        {:style styles/margin-top-gutter-more}
        [action-form
         {:offering @offering}]]])))

(defn similar-offerings []
  (let [search-results (subscribe [:offerings/similar-offerings])]
    (fn []
      (let [{:keys [:items :loading? :params :total-count]} @search-results]
        [paper
         {:style styles/search-results-paper-secondary}
         [:h1
          {:style styles/search-results-paper-headline}
          "Similar Offerings"]
         [search-results-infinite-list
          {:total-count total-count
           :offset (:offset params)
           :loading? loading?
           :no-items-text "No similar offerings found"
           :on-next-load (fn [offset limit]
                           (dispatch [:offerings.ens-record-offerings/set-params-and-search {:offset offset :limit limit}]))}
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
        [side-nav-menu-center-layout
         [paper
          [:h1
           {:style styles/page-headline}
           "Offering " (:offering/name offering)]
          (if offering-loaded?
            [offering-detail]
            [list-item-placeholder])]
         [similar-offerings]]))))
