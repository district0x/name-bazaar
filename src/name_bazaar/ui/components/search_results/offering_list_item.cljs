(ns name-bazaar.ui.components.search-results.offering-list-item
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils :refer [epoch->long empty-address?]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.text-field :refer [ether-field-with-currency]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code truncate current-component-mui-theme format-time-duration-units format-local-datetime time-ago]]
    [name-bazaar.shared.utils :refer [calculate-min-bid]]
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
    {:route :route.offering/detail
     :route-params {:offering/address address}}
    "Open Offering Detail"]])

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
   "WARNING: Original owner did not transfer or removed ownership of this name from offering contract."
   "For this reason buying or bidding for this name is not possible."])

(defn new-owner-info [props]
  (let [active-address (subscribe [:district0x/active-address])
        my-addresses (subscribe [:district0x/my-addresses])]
    (fn [{:keys [:offering/new-owner]}]
      [:div
       (r/merge-props
         {:style {:font-size "1.2em"}}
         (dissoc props :offering/new-owner))
       (cond
         (= new-owner @active-address)
         "Congratulations, you bought this offering"

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

(defn action-form [{:keys [:offering]} & children]
  (let [{:keys [:offering/address :offering/new-owner :offering/type :auction-offering/bid-count]} offering]
    (if (empty-address? new-owner)
      (if @(subscribe [:offering/active-address-original-owner? address])
        [ui/raised-button
         {:primary true
          :label "Edit"
          :href (path-for :route.offering/edit {:offering/address address})
          :disabled (and (= type :auction-offering) (pos? bid-count))}]
        (into [:div] children))
      [new-owner-info
       {:offering/new-owner new-owner}])))

(defn buy-now-form [{:keys [:offering] :as props}]
  (let [{:keys [:offering/price :offering/address :offering/new-owner]} offering]
    [row
     {:end "xs"
      :bottom "xs"
      :style styles/full-height}
     [action-form
      {:offering offering}
      [ui/raised-button
       {:primary true
        :label "Buy"
        :disabled (not @(subscribe [:offering/node-owner? address]))
        :on-click #(dispatch [:buy-now-offering/buy {:offering/address address
                                                     :offering/price price}])}]]]))

(defn auction-bid-info [{:keys [:min-bid :on-min-bid-click :pending-returns :active-address-winning? :offering/address]}]
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
         {:on-click #(on-min-bid-click min-bid)}
         (format-eth-with-code min-bid)]
        " to beat the highest bid."]
       [:div "You need to send at least "
        [:a
         {:on-click #(on-min-bid-click min-bid)}
         (format-eth-with-code min-bid)]
        " to become higest bidder."])
     [:div "To place a bid, you can send Ether directly into "
      [d0x-misc/etherscan-link
       {:address address}
       "offering address"]
      " or you can use the form below."]]
    [col
     {:xs 12}
     "Your bid is currently winning this auction!"]))

(defn auction-bid-form []
  (let [bid-form (r/atom {:value nil :valid? true})]
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
             :offering/address address}])
         [col
          {:xs 12
           :style (merge styles/text-right
                         styles/margin-top-gutter-less)}
          [action-form
           {:offering offering}
           [ether-field-with-currency
            {:value (or (:value @bid-form) min-bid)
             :floating-label-text "Your bid"
             :on-change (fn [_ value valid?]
                          (reset! bid-form {:value value :valid? valid?}))}]
           [ui/raised-button
            {:primary true
             :label "Bid"
             :style {:margin-left styles/desktop-gutter-less}
             :disabled (or (not (:valid? @bid-form))
                           (not @(subscribe [:offering/node-owner? address]))
                           active-address-winning?)
             :on-click #(dispatch [:auction-offering/bid {:offering/address address
                                                          :offering/price (or (:value @bid-form) min-bid)}])}]]]]))))

(defn offering-expanded-body [{:keys [:offering]}]
  (let [{:strs [desktopGutter desktopGutterLess]} (current-component-mui-theme "spacing")
        {:keys [:offering/created-on :offering/type :auction-offering/min-bid-increase :offering/new-owner
                :auction-offering/extension-duration :auction-offering/winning-bidder :offering/original-owner
                :offering/address :offering/name :auction-offering/end-time :offering/contains-non-ascii?]} offering
        registrar-entry @(subscribe [:offering/registrar-entry address])]
    [row-with-cols
     {:style styles/search-results-list-item-body}
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
                            (truncate original-owner 20)]
       (when @(subscribe [:offering/active-address-original-owner? address])
         " (You)")]
      [:div "Offering Address: " [d0x-misc/etherscan-link {:address address}
                                  (truncate address 20)]]
      [:div
       "Locked Value: " [d0x-misc/etherscan-link
                         {:address (:registrar.entry.deed/address registrar-entry)}
                         (format-eth-with-code (:registrar.entry.deed/value registrar-entry))]]]
     [col
      {:xs 12 :md 4
       :style styles/text-right}
      [open-offering-detail-button
       {:offering/address address}]
      [open-name-in-etherscan-button
       {:name name}]
      [add-to-watched-names-button
       {:name name}]]
     (when (and (false? @(subscribe [:offering/node-owner? address]))
                (empty-address? new-owner))
       [col
        {:xs 12 :style styles/margin-top-gutter-mini}
        [not-node-owner-warning]])
     (when contains-non-ascii?
       [col
        {:xs 12 :style styles/margin-top-gutter-mini}
        [non-ascii-characters-warning]])
     [col
      {:xs 12
       :style styles/margin-top-gutter-mini}
      (if (= type :auction-offering)
        [auction-bid-form
         {:offering offering}]
        [buy-now-form
         {:offering offering}])]]))

(defn offering-list-item []
  (fn [{:keys [:offering :show-bid-count? :show-time-left? :expanded? :on-expand :key]}]
    (let [{:keys [:offering/address :offering/type :offering/name :offering/price
                  :auction-offering/bid-count :auction-offering/end-time]} offering
          {:strs [desktopGutterMini desktopGutterLess]} (current-component-mui-theme "spacing")]
      [expandable-list-item
       {:index key
        :on-collapse #(dispatch [:offering-list-item-collapsed offering])
        :on-expand #(dispatch [:offering-list-item-expanded offering])
        :collapsed-height styles/search-results-list-item-height
        :expanded-height (if (= type :auction-offering)
                           styles/auction-offering-list-item-expanded-height
                           styles/buy-now-offering-list-item-expanded-height)
        :expand-disabled? (not address)}
       [:div
        {:style styles/search-results-list-item}
        (if-not address
          [list-item-placeholder]
          [row-with-cols
           {:between "md"
            :middle "md"}
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
              (format-eth-with-code price)]]]])]
       [offering-expanded-body
        {:offering offering}]])))
