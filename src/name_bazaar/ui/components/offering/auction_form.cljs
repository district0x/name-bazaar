(ns name-bazaar.ui.components.offering.auction-form
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.shared.utils :as d0x-shared-utils :refer [empty-address?]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col page]]
    [district0x.ui.components.text-field :refer [ether-field-with-currency]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code]]
    [name-bazaar.shared.utils :refer [calculate-min-bid]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.components.offering.auction-finalize-button :refer [auction-finalize-button]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-time.core :as t]))

(defn auction-bid-info [{:keys [:min-bid :on-min-bid-click :pending-returns :active-address-winning? :offering/address
                                :offering/new-owner]}]
  (let [offering-status @(subscribe [:offering/status address])
        active? (= offering-status :offering.status/active)]
    (if-not active-address-winning?
      [:div
       {:style styles/full-width}
       (if (pos? pending-returns)
         [:div
          "You have pending returns: " (format-eth-with-code pending-returns)
          ". ("
          (if-not @(subscribe [:auction-offering.withdraw/tx-pending? address])
            [:a
             {:on-click #(dispatch [:auction-offering/withdraw {:offering/address address}])}
             "withdraw"]
            "withdrawing...")
          ")"]
         [:div "You have no pending returns."])
       (when active?
         (if (pos? pending-returns)
           [:div
            "For your next bid it's enough for you to send "
            [:a
             {:on-click #(on-min-bid-click min-bid)}
             (format-eth-with-code min-bid)]
            " to beat the highest bid."]
           [:div "You need to send at least "
            [:a
             {:on-click #(on-min-bid-click min-bid)}
             (format-eth-with-code min-bid)]
            " to become higest bidder."]))
       (when active?
         [:div "To place a bid, you can send Ether directly into "
          [d0x-misc/etherscan-link
           {:address address}
           "offering address"]
          " or you can use the form below."])]
      [:div
       {:style styles/full-width}
       "Your bid is winning this auction!"])))

(defn auction-form []
  (let [bid-value (r/atom nil)
        xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering] :as props}]
      (let [{:keys [:offering/address :offering/price :auction-offering/min-bid-increase
                    :auction-offering/bid-count :offering/new-owner :auction-offering/end-time]} offering
            offering-status @(subscribe [:offering/status address])
            pending-returns (or @(subscribe [:auction-offering/active-address-pending-returns address]) 0)
            active-address-winning? @(subscribe [:auction-offering/active-address-winning-bidder? address])
            min-bid (calculate-min-bid price min-bid-increase bid-count pending-returns)]
        [row
         (r/merge-props
           {:style (merge styles/full-height
                          styles/full-width)
            :bottom "xs"}
           (dissoc props :offering))
         (when-not @(subscribe [:offering/active-address-original-owner? address])
           [auction-bid-info
            {:min-bid min-bid
             :on-min-bid-click #(reset! bid-value nil)
             :pending-returns pending-returns
             :active-address-winning? active-address-winning?
             :offering/address address
             :offering/new-owner new-owner}])
         [:div
          {:style (merge styles/text-right
                         styles/full-width
                         styles/margin-top-gutter-less
                         (when @xs?
                           styles/text-left))}
          (when (and active-address-winning?
                     (= offering-status :offering.status/auction-ended))
            [row
             {:bottom "xs" :end "xs"}
             [auction-finalize-button
              {:offering offering}]])
          (when-not (contains? #{:offering.status/auction-ended :offering.status/finalized} offering-status)
            [row
             {:bottom "xs" :end "xs"}
             [ether-field-with-currency
              {:value (or @bid-value min-bid)
               :floating-label-text "Your bid"
               :full-width @xs?
               :on-change #(reset! bid-value %2)}]
             [raised-transaction-button
              {:primary true
               :label "Bid"
               :full-width @xs?
               :pending? @(subscribe [:auction-offering.bid/tx-pending? address])
               :pending-label "Bidding..."
               :style (merge
                        (if-not @xs?
                          {:margin-bottom styles/desktop-gutter-mini
                           :margin-left styles/desktop-gutter-less}
                          styles/margin-top-gutter-mini))
               :disabled (or (not (>= (or @bid-value min-bid) min-bid))
                             (= offering-status :offering.status/missing-ownership)
                             active-address-winning?)
               :on-click #(dispatch [:auction-offering/bid {:offering/address address
                                                            :offering/price (or @bid-value min-bid)}])}]])]]))))
