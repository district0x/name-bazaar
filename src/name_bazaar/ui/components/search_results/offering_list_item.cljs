(ns name-bazaar.ui.components.search-results.offering-list-item
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
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def offering-type->text
  {:buy-now-offering "Buy Now"
   :auction-offering "Auction"})

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

(defn non-ascii-characters-warning [props]
  [:div
   (r/merge-props
     {:style styles/warning-color}
     (dissoc props))
   "WARNING: This name contains non-latin characters. Some of these characters may look same or "
   "very similar to their standard latin counterparts. If you're unsure what this means, please contact "
   "our team."])

(defn not-node-owner-warning [props]
  [:div
   (r/merge-props
     {:style styles/warning-color}
     props)
   "WARNING: Original owner removed or haven't transferred ownership of this name from offering contract. "
   "For this reason buying or bidding for this name is not possible."])

(defn sub-level-name-warning [{:keys [:offering/name] :as props}]
  (let [parent-name (string/replace name (name-label name) "")]
    [:div
     (r/merge-props
       {:style styles/warning-color}
       props)
     "WARNING: This is not top level name. Beware, that owner of " parent-name " will be always able to take this "
     "name back. Buy only when you trust a owner of " parent-name "."]))

(defn new-owner-info [props]
  (let [active-address (subscribe [:district0x/active-address])
        my-addresses (subscribe [:district0x/my-addresses])]
    (fn [{:keys [:offering/new-owner]}]
      [:div
       (r/merge-props
         {:style styles/offering-list-item-new-owner-info}
         (dissoc props :offering/new-owner))
       (cond
         (= new-owner @active-address)
         "Congratulations, you bought this offering!"

         (contains? (set @my-addresses) new-owner)
         [:span "This offering was bought by one of your addresses ("
          [a {:route :route.user/purchases
              :route-params {:user/address new-owner}}
           (truncate new-owner 20)]
          ")"]

         :else
         [:span "This offering was bought by " [a {:route :route.user/purchases
                                                   :route-params {:user/address new-owner}}
                                                (truncate new-owner 20)]])])))

(defn transaction-form [{:keys [:offering]} & children]
  (let [{:keys [:offering/address :offering/new-owner :offering/type :auction-offering/bid-count]} offering]
    (if (empty-address? new-owner)
      (if @(subscribe [:offering/active-address-original-owner? address])
        [ui/raised-button
         {:primary true
          :label "Edit"
          :href (path-for :route.offerings/edit {:offering/address address})
          :disabled (and (= type :auction-offering) (pos? bid-count))}]
        (when-not (false? @(subscribe [:offering/node-owner? address]))
          (into [:div] children)))
      [new-owner-info
       {:offering/new-owner new-owner}])))

(defn buy-now-form [{:keys [:offering] :as props}]
  (let [{:keys [:offering/price :offering/address :offering/new-owner]} offering]
    [row
     {:end "xs"
      :bottom "xs"
      :style styles/full-height}
     [transaction-form
      {:offering offering}
      [raised-transaction-button
       {:primary true
        :label "Buy"
        :pending? @(subscribe [:buy-now-offering/buy-tx-pending? address])
        :pending-label "Buying..."
        :disabled (not @(subscribe [:offering/node-owner? address]))
        :on-click #(dispatch [:buy-now-offering/buy {:offering/address address
                                                     :offering/price price}])}]]]))

(defn auction-bid-info [{:keys [:min-bid :on-min-bid-click :pending-returns :active-address-winning? :offering/address
                                :offering/new-owner]}]
  (let [bidding-disabled? (and (false? @(subscribe [:offering/node-owner? address])) (empty-address? new-owner))]
    (if-not active-address-winning?
      [col
       {:xs 12}
       (if (pos? pending-returns)
         [:div
          "You have pending returns: " (format-eth-with-code pending-returns)
          ". ("
          (if-not @(subscribe [:auction-offering/withdraw-tx-pending? address])
            [:a
             {:on-click #(dispatch [:auction-offering/withdraw {:offering/address address}])}
             "withdraw"]
            "withdrawing...")
          ")"]
         [:div "You have no pending returns."])
       (when-not bidding-disabled?
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
       (when-not bidding-disabled?
         [:div "To place a bid, you can send Ether directly into "
          [d0x-misc/etherscan-link
           {:address address}
           "offering address"]
          " or you can use the form below."])]
      [col
       {:xs 12}
       "Your bid is currently winning this auction!"])))

(defn auction-bid-form []
  (let [bid-form (r/atom {:value nil :valid? true})
        xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering] :as props}]
      (let [{:keys [:offering/address :offering/price :auction-offering/min-bid-increase
                    :auction-offering/bid-count :offering/new-owner]} offering
            pending-returns (or @(subscribe [:auction-offering/active-address-pending-returns address]) 0)
            active-address-winning? @(subscribe [:auction-offering/active-address-winning-bidder? address])
            min-bid (calculate-min-bid price min-bid-increase bid-count pending-returns)]
        [row-with-cols
         (r/merge-props
           {:style styles/full-height
            :bottom "xs"}
           (dissoc props :offering))
         (when-not @(subscribe [:offering/active-address-original-owner? address])
           [auction-bid-info
            {:min-bid min-bid
             :on-min-bid-click #(reset! bid-form {:value nil :valid? true})
             :pending-returns pending-returns
             :active-address-winning? active-address-winning?
             :offering/address address
             :offering/new-owner new-owner}])
         [col
          {:xs 12
           :style (merge styles/text-right
                         styles/margin-top-gutter-less
                         (when @xs?
                           styles/text-left))}
          [transaction-form
           {:offering offering}
           [row
            {:bottom "xs"
             :end "xs"}
            [ether-field-with-currency
             {:value (or (:value @bid-form) min-bid)
              :floating-label-text "Your bid"
              :full-width @xs?
              :on-change (fn [_ value valid?]
                           (reset! bid-form {:value value :valid? valid?}))}]
            [raised-transaction-button
             {:primary true
              :label "Bid"
              :full-width @xs?
              :pending? @(subscribe [:auction-offering/bid-tx-pending? address])
              :pending-label "Bidding..."
              :style (merge
                       (if-not @xs?
                         {:margin-bottom styles/desktop-gutter-mini
                          :margin-left styles/desktop-gutter-less}
                         styles/margin-top-gutter-mini))
              :disabled (or (not (:valid? @bid-form))
                            (not @(subscribe [:offering/node-owner? address]))
                            active-address-winning?)
              :on-click #(dispatch [:auction-offering/bid {:offering/address address
                                                           :offering/price (or (:value @bid-form) min-bid)}])}]]]]]))))

(defn offering-name-line [{:keys [:offering/name]}]
  [:div "Name: " [a {:route :route.ens-record/detail
                     :route-params {:ens.record/name name}}
                  name]])

(defn offering-created-on-line [{:keys [:offering/created-on]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Created on " (format-local-datetime created-on)
   "(" (time-ago created-on) ")"])

(defn auction-offering-winning-bidder-line [{:keys [:auction-offering/winning-bidder]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Winning bidder: " (if winning-bidder
                        [a {:route :route.user/bids
                            :route-params {:user/address winning-bidder}}
                         (truncate winning-bidder 20)]
                        "none")])

(defn offering-original-owner-line [{:keys [:offering/original-owner :offering/address]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Offered by"
   (when @(subscribe [:offering/active-address-original-owner? address]) " (You)")
   ": "
   [a {:route :route.user/offerings
       :route-params {:user/address original-owner}}
    original-owner]])

(defn offering-address-line [{:keys [:offering/address]}]
  [:div
   {:style styles/text-overflow-ellipsis}
   "Offering Address: " [d0x-misc/etherscan-link {:address address} address]])

(defn registrar-entry-deed-value-line [{:keys [:registrar.entry.deed/address :registrar.entry.deed/value]}]
  [:div
   "Locked Value: " [d0x-misc/etherscan-link
                     {:address address}
                     (format-eth-with-code value)]])

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
          [offering-name-line
           {:offering/name name}]
          [offering-created-on-line
           {:offering/created-on created-on}]
          (when (= type :auction-offering)
            [:div "Ends on " (format-local-datetime end-time)])
          (when (= type :auction-offering)
            [:div "Min. Bid Increase: " (format-eth-with-code min-bid-increase)])
          (when (= type :auction-offering)
            [:div "Time Extension: "
             (d0x-ui-utils/format-time-duration-units (epoch->long extension-duration))])
          (when (= type :auction-offering)
            [auction-offering-winning-bidder-line
             {:auction-offering/winning-bidder winning-bidder}])
          [offering-original-owner-line
           {:offering/original-owner original-owner
            :offering/address address}]
          [offering-address-line
           {:offering/address address}]
          [registrar-entry-deed-value-line
           registrar-entry]]
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
            (and (false? @(subscribe [:offering/node-owner? address])) (empty-address? new-owner))
            [not-node-owner-warning]

            (> name-level 1)
            [sub-level-name-warning
             {:offering/name name}]

            contains-non-ascii?
            [non-ascii-characters-warning])]

         [col
          {:xs 12
           :style styles/margin-top-gutter-mini}
          (if (= type :auction-offering)
            [auction-bid-form
             {:offering offering}]
            [buy-now-form
             {:offering offering}])]]))))

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
          :on-collapse #(dispatch [:offering-list-item-collapsed offering])
          :on-expand #(dispatch [:offering-list-item-expanded offering])
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
