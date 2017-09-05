(ns name-bazaar.ui.components.offering.list-item
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils :refer [epoch->long empty-address?]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.text-field :refer [ether-field-with-currency]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code truncate current-component-mui-theme format-time-duration-units format-local-datetime time-ago]]
    [name-bazaar.shared.utils :refer [calculate-min-bid name-label]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.components.offering.action-form :refer [action-form]]
    [name-bazaar.ui.components.offering.general-info :refer [offering-general-info]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.components.offering.warnings :refer [non-ascii-characters-warning missing-ownership-warning sub-level-name-warning]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url path-for offering-type->text]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn open-offering-detail-button [{:keys [:offering/address]}]
  [:div
   [a
    {:route :route.offerings/detail
     :route-params {:offering/address address}
     :style styles/text-decor-none}
    "Open Offering Detail"]])

(defn open-name-in-etherscan-button [{:keys [:offering/name]}]
  [:div
   [:a
    {:href (etherscan-ens-url name)
     :target :_blank
     :style styles/text-decor-none}
    "Open in Etherscan"]])

(defn add-to-watched-names-button [{:keys [:offering/name]}]
  [:div
   [:a
    {:style styles/text-decor-none
     :on-click #(dispatch [:watched-names/add name])}
    "Add to Watched Names"]])

(defn offering-expanded-body []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering]}]
      (let [{:strs [desktopGutter desktopGutterLess]} (current-component-mui-theme "spacing")
            {:keys [:offering/created-on :offering/type :auction-offering/min-bid-increase :offering/new-owner
                    :auction-offering/extension-duration :auction-offering/winning-bidder :offering/original-owner
                    :offering/address :offering/name :auction-offering/end-time :offering/contains-non-ascii?
                    :offering/name-level]} offering
            registrar-entry @(subscribe [:offering/registrar-entry address])]
        [row-with-cols
         {:style (styles/search-results-list-item-body @xs?)}
         [col
          {:xs 12 :sm 8 :style styles/margin-bottom-gutter-mini}
          [offering-general-info
           {:offering offering}]]
         [col
          {:xs 12 :sm 4
           :style (merge
                    styles/margin-bottom-gutter-mini
                    (if @xs? styles/text-left styles/text-right))}
          [open-offering-detail-button
           {:offering/address address}]
          [open-name-in-etherscan-button
           {:offering/name name}]
          [add-to-watched-names-button
           {:offering/name name}]]
         [col
          {:xs 12}
          (cond
            @(subscribe [:offering/show-missing-ownership-warning? address])
            [missing-ownership-warning]

            (> name-level 1)
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
     (d0x-ui-utils/format-time-remaining-biggest-unit end-time) " left"]))

(defn offering-list-item []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering :show-bid-count? :show-time-left? :expanded? :on-expand :key]}]
      (let [{:keys [:offering/address :offering/type :offering/name :offering/price
                    :auction-offering/bid-count :auction-offering/end-time]} offering
            {:strs [desktopGutterMini desktopGutterLess]} (current-component-mui-theme "spacing")]
        [expandable-list-item
         {:index key
          :on-collapse #(dispatch [:offerings/list-item-collapsed offering])
          :on-expand #(dispatch [:offerings/list-item-expanded offering])
          :collapsed-height (styles/search-results-list-item-height @xs?)
          :expanded-height (if (= type :auction-offering)
                             (styles/auction-offering-list-item-expanded-height @xs?)
                             (styles/buy-now-offering-list-item-expanded-height @xs?))
          :expand-disabled? (not address)}
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
            [:div
             {:style styles/offering-list-item-name}
             name]]
           (when (and (not @xs?) show-bid-count? (= type :auction-offering))
             [col
              {:sm 2
               :style styles/text-center}
              [auction-bid-count
               {:auction-offering/bid-count bid-count}]])
           (when (and (not @xs?) show-time-left? (= type :auction-offering))
             [col
              {:sm 2
               :style styles/text-center}
              [auction-time-remaining
               {:auction-offering/end-time end-time}]])
           [col
            {:xs 6 :sm 3}
            [:div
             {:style (styles/offering-list-item-price @xs?)}
             (format-eth-with-code price)]]
           (when (and @xs? (= type :auction-offering))
             [col
              {:xs 6
               :style styles/offering-list-item-bid-count-xs}
              [row
               {:bottom "xs"
                :end "xs"
                :style styles/full-height}
               [auction-bid-count
                {:auction-offering/bid-count bid-count}] ", "
               [auction-time-remaining
                {:auction-offering/end-time end-time}]]])]]
         [offering-expanded-body
          {:offering offering}]]))))
