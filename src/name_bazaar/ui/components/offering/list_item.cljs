(ns name-bazaar.ui.components.offering.list-item
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.shared.utils :refer [empty-address?]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col paper]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code format-local-datetime]]
    [name-bazaar.ui.components.add-to-watched-names-button :refer [add-to-watched-names-button]]
    [name-bazaar.ui.components.ens-record.etherscan-link :refer [ens-record-etherscan-link]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.components.offering.action-form :refer [action-form]]
    [name-bazaar.ui.components.offering.chips :refer [offering-active-chip offering-sold-chip offering-bought-chip]]
    [name-bazaar.ui.components.offering.general-info :refer [offering-general-info]]
    [name-bazaar.ui.components.offering.warnings :refer [non-ascii-characters-warning missing-ownership-warning sub-level-name-warning]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url path-for offering-type->text]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-time.core :as t]))

(defn offering-detail-link [{:keys [:offering/address]}]
  [:div
   [a
    {:route :route.offerings/detail
     :route-params {:offering/address address}
     :style styles/text-decor-none}
    "Open Offering Detail"]])

(defn offering-expanded-body []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering]}]
      (let [{:keys [:offering/address :offering/name :offering/contains-non-ascii? :offering/top-level-name?
                    :offering/original-owner]} offering
            show-missing-ownership-warning? @(subscribe [:offering/show-missing-ownership-warning? address])]
        [row-with-cols
         {:style (styles/search-results-list-item-body @xs?)}
         [col
          {:xs 12 :sm 8 :style styles/margin-bottom-gutter-mini}
          [offering-general-info
           {:offering offering}]]
         [col
          {:xs 12 :sm 4
           :style (styles/list-item-body-links-container @xs?)}
          [offering-detail-link
           {:offering/address address}]
          [ens-record-etherscan-link
           {:ens.record/name name}]
          [add-to-watched-names-button
           {:ens.record/name name}]]
         [col
          {:xs 12}
          (cond
            show-missing-ownership-warning?
            [missing-ownership-warning
             {:offering/original-owner original-owner}]

            (not top-level-name?)
            [sub-level-name-warning
             {:offering/name name}]

            contains-non-ascii?
            [non-ascii-characters-warning])]

         [col
          {:xs 12
           :style (merge styles/margin-top-gutter-mini
                         styles/text-right
                         styles/full-width)}
          [action-form
           {:offering offering}]]]))))

(defn auction-bid-count [{:keys [:auction-offering/bid-count]}]
  (when bid-count
    [:span
     {:style styles/offering-list-item-time-left}
     bid-count " " (d0x-ui-utils/pluralize "bid" bid-count)]))

(defn auction-time-remaining [{:keys [:auction-offering/end-time]}]
  (when end-time
    [:span
     {:style styles/offering-list-item-time-left}
     (if (t/after? end-time (t/now))
       (str (d0x-ui-utils/format-time-remaining-biggest-unit end-time) " left")
       "finished")]))

(defn offering-list-item-header []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering :show-created-on? :show-sold-for? :show-bought-for? :show-active?]}]
      (let [{:keys [:offering/address :offering/auction? :offering/name :offering/price :offering/created-on
                    :offering/new-owner :auction-offering/bid-count :auction-offering/end-time]} offering]
        [:div
         {:style (styles/search-results-list-item @xs?)}
         (when-not address
           [list-item-placeholder])
         [row-with-cols
          {:style (merge styles/search-results-list-item-header
                         (if address styles/opacity-1 styles/opacity-0))
           :between "sm"
           :middle "sm"}
          [col
           {:xs 12 :sm 5}
           [:div
            {:style styles/offering-list-item-type}
            (offering-type->text type)]
           (if show-created-on?
             [:div
              {:style styles/list-item-ens-record-name}
              (format-local-datetime created-on)]
             [:div
              {:style styles/list-item-ens-record-name}
              name])]
          (when (and (not @xs?) auction?)
            [col
             {:sm 2
              :style styles/text-center}
             [auction-bid-count
              {:auction-offering/bid-count bid-count}]])
          (when (and (not @xs?) auction?)
            [col
             {:sm 2
              :style styles/text-center}
             [auction-time-remaining
              {:auction-offering/end-time end-time}]])
          [col
           {:xs 4 :sm 3}
           [:div
            {:style (styles/offering-list-item-header-last-col @xs?)}
            (when (and (not @xs?) show-active? @(subscribe [:offering/active? address]))
              [offering-active-chip
               {:style {:margin-right 5}}])
            (when (and
                    (not @xs?)
                    (or show-sold-for? show-bought-for?)
                    (not (empty-address? new-owner)))
              [:span
               {:style styles/offering-list-item-price-leading-text}
               (cond
                 show-sold-for? "sold for "
                 show-bought-for? "bought for ")])
            [:span
             {:style (styles/offering-list-item-price @xs?)}
             (format-eth-with-code price)]]]
          (when @xs?
            [col
             {:xs 8
              :style styles/offering-list-item-bid-count-xs}
             [row
              {:bottom "xs"
               :end "xs"
               :style styles/full-height}
              (when auction?
                [:div
                 [auction-bid-count
                  {:auction-offering/bid-count bid-count}]
                 [:span
                  {:style {:margin-right 3}}
                  ", "]
                 [auction-time-remaining
                  {:auction-offering/end-time end-time}]])
              (when (and show-active? @(subscribe [:offering/active? address]))
                [offering-active-chip
                 {:style styles/margin-left-gutter-mini}])
              (when (and (or show-sold-for? show-bought-for?)
                         (not (empty-address? new-owner)))
                (cond
                  show-sold-for? [offering-sold-chip
                                  {:style styles/margin-left-gutter-mini}]
                  show-bought-for? [offering-bought-chip
                                    {:style styles/margin-left-gutter-mini}]))]])]]))))

(defn offering-list-item []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering :expanded? :on-expand :key :header-props]}]
      (let [{:keys [:offering/address :offering/auction?]} offering]
        [expandable-list-item
         {:index key
          :on-collapse #(dispatch [:offerings.list-item/collapsed offering])
          :on-expand #(dispatch [:offerings.list-item/expanded offering])
          :collapsed-height (styles/search-results-list-item-height @xs?)
          :expanded-height (if auction?
                             (styles/auction-offering-list-item-expanded-height @xs?)
                             (styles/buy-now-offering-list-item-expanded-height @xs?))
          :expand-disabled? (not address)}
         [offering-list-item-header
          (merge {:offering offering}
                 header-props)]
         [offering-expanded-body
          {:offering offering}]]))))
