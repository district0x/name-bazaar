(ns components.offering-list-item
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils :refer [epoch->long]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.text-field :refer [ether-field-with-currency]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code truncate current-component-mui-theme format-time-duration-units format-local-datetime time-ago]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def offering-type->text
  {:buy-now-offering "Buy Now"
   :auction-offering "Auction"})

(defn open-name-in-etherscan-button [{:keys [:name]}]
  [:div
   [:a
    {:href (etherscan-ens-url name)
     :target :_blank}
    "Open in Etherscan"]])

(defn add-to-watched-names-button [{:keys [:name]}]
  [:div
   [:a
    {:on-click #(dispatch [:watched-names/add name])}
    "Add to Watched Names"]])

(defn non-ascii-characters-warning [{:keys [:offering] :as props}]
  (when (:offering/contains-non-ascii? offering)
    [:div
     (r/merge-props
       {:style styles/warning-color}
       (dissoc props :offering))
     "WARNING: This name contains non-latin characters. Some of these characters may look same or "
     "very similar to their standard latin counterparts. If you're unsure what this means, please contact "
     "our team."]))

(defn not-node-owner-warning [{:keys [:offering/address] :as props}]
  (when (false? @(subscribe [:offering/node-owner? address]))
    [:div
     (r/merge-props
       {:style styles/warning-color}
       (dissoc props :offering/address))
     "WARNING: Original owner did not transfer or removed ownership of this name from offering contract."
     "For this reason buying or bidding for this name is not possible."]))

(defn auction-bid-form []
  (let [bid-value (r/atom nil)
        valid-value? (r/atom true)]
    (fn [{:keys [:offering] :as props}]
      (let [{:keys [:offering/address :offering/price :auction-offering/min-bid-increase
                    :auction-offering/bid-count]} offering
            pending-returns (or @(subscribe [:auction-offering/active-address-pending-returns address]) 0)
            active-address-winning? @(subscribe [:auction-offering/active-address-winning-bidder? address])
            min-bid (- (+ price (when (pos? bid-count) min-bid-increase)) pending-returns)]
        [row-with-cols
         (dissoc props :offering)
         (if-not active-address-winning?
           [col
            {:xs 12}
            (if (pos? pending-returns)
              [:div
               "You have pending returns: " (format-eth-with-code pending-returns)
               ". ("
               [:a
                {:on-click #(dispatch [:auction-offering/withdraw {:offering/address address}])}
                "withdraw"]
               ")"]
              [:div "You have no pending returns."])
            (if (pos? pending-returns)
              [:div
               "For your next bid it's enough for you to send "
               [:a
                {:on-click (fn []
                             (reset! bid-value min-bid)
                             (reset! valid-value? true))}
                (format-eth-with-code min-bid)]
               " to beat the highest bid."]
              [:div "You need to send at least "
               [:a
                {:on-click (fn []
                             (reset! bid-value min-bid)
                             (reset! valid-value? true))}
                (format-eth-with-code min-bid)]
               " to become higest bidder."])
            [:div "To place a bid, you can send Ether directly into "
             [d0x-misc/etherscan-link
              {:address address}
              "offering address"]
             " or you can use our form below."]]
           [col
            {:xs 12}
            "Your bid is currently winning this auction."])
         [col
          {:xs 12
           :style (merge styles/text-right
                         styles/margin-top-gutter-less)}
          [ether-field-with-currency
           {:value (or @bid-value min-bid)
            :floating-label-text "Your bid"
            :on-change (fn [_ value valid?]
                         (reset! bid-value value)
                         (reset! valid-value? valid?))}]
          [ui/raised-button
           {:primary true
            :label "Bid"
            :style {:margin-left styles/desktop-gutter-less}
            :disabled (or (not @valid-value?)
                          (not @(subscribe [:offering/node-owner? address]))
                          active-address-winning?)
            :on-click #(dispatch [:auction-offering/bid {:offering/address address
                                                         :offering/price (or @bid-value min-bid)}])}]]]))))

(defn offering-expanded-body [{:keys [:offering]}]
  (let [{:strs [desktopGutter desktopGutterLess]} (current-component-mui-theme "spacing")
        {:keys [:offering/created-on :offering/type :auction-offering/min-bid-increase
                :auction-offering/extension-duration :auction-offering/winning-bidder :offering/original-owner
                :offering/address :offering/name :auction-offering/end-time]} offering
        registrar-entry @(subscribe [:offering/registrar-entry address])]
    [row-with-cols
     {:style {:padding desktopGutterLess}}
     [col
      {:xs 12 :md 8}
      [:div "Created on "
       (format-local-datetime created-on)
       "(" (time-ago created-on) ")"]
      (when (= type :auction-offering)
        [:div "Ends on " (format-local-datetime end-time)])
      (when (= type :auction-offering)
        [:div "Min. Bid Increase: " (format-eth-with-code min-bid-increase)])
      (when (= type :auction-offering)
        [:div "Time Extension: "
         (d0x-ui-utils/format-time-duration-units (epoch->long extension-duration))])
      (when (= type :auction-offering)
        [:div "Winning bidder: " (if winning-bidder
                                   [a {:route :route.user/bids
                                       :route-params {:user/address winning-bidder}}
                                    (truncate winning-bidder 20)]
                                   "none")])
      [:div "Offered by: " [a {:route :route.user/offerings
                               :route-params {:user/address original-owner}}
                            (truncate original-owner 20)]]
      [:div "Offering Address: " [d0x-misc/etherscan-link {:address address}
                                  (truncate address 20)]]
      [:div
       "Locked Value: " [d0x-misc/etherscan-link
                         {:address (:registrar.entry.deed/address registrar-entry)}
                         (format-eth-with-code (:registrar.entry.deed/value registrar-entry))]]]
     [col
      {:xs 12 :md 4
       :style styles/text-right}
      [open-name-in-etherscan-button
       {:name name}]
      [add-to-watched-names-button
       {:name name}]]
     [col
      {:xs 12}
      [not-node-owner-warning
       {:offering/address address
        :style styles/margin-top-gutter-less}]]
     [col
      {:xs 12}
      [non-ascii-characters-warning
       {:offering offering
        :style styles/margin-top-gutter-less}]]
     [col
      {:xs 12}
      (if (= type :auction-offering)
        [auction-bid-form
         {:style styles/margin-top-gutter-less
          :offering offering}])]]))

(defn offering-list-item []
  (fn [{:keys [:offering :show-bid-count? :show-time-left? :expanded? :on-expand :key]}]
    (let [{:keys [:offering/address :offering/type :offering/name :offering/price
                  :auction-offering/bid-count :auction-offering/end-time]} offering
          {:strs [desktopGutterMini desktopGutterLess]} (current-component-mui-theme "spacing")]
      [expandable-list-item
       {:index key
        :on-collapse #(dispatch [:offering-collapsed offering])
        :on-expand #(dispatch [:offering-expanded offering])
        :collapsed-height 52
        :expanded-height 400}
       [row-with-cols
        {:between "md"
         :middle "md"
         :style {:padding-top desktopGutterMini
                 :padding-bottom desktopGutterMini
                 :padding-left desktopGutterLess
                 :padding-right desktopGutterLess}}
        [col
         {:md 5}
         [:div
          {:style {:font-size "0.75em"}}
          (offering-type->text type)]
         [:div
          {:style {:font-size "1.3em"
                   :overflow :hidden
                   :text-overflow :ellipsis
                   :white-space :nowrap}}
          name]]
        (when (and show-bid-count? (= type :auction-offering))
          [col
           {:md 2
            :style {:text-align "center"
                    :font-size "1.05em"}}
           (when bid-count
             [:div
              bid-count " " (d0x-ui-utils/pluralize "bid" bid-count)])])
        (when (and show-time-left? (= type :auction-offering))
          [col
           {:md 2
            :style {:text-align "center"
                    :font-size "1.05em"}}
           (when end-time
             [:div
              (d0x-ui-utils/format-time-remaining-biggest-unit end-time) " left"])])
        [col
         {:md 3
          :style {:text-align "right"}}
         [row
          {:middle "xs"
           :end "xs"}
          [:div
           {:style {:font-size "1.3em"}}
           (format-eth-with-code price)]]]]
       [offering-expanded-body
        {:offering offering}]])))
