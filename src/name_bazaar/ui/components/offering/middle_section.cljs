(ns name-bazaar.ui.components.offering.middle-section
  (:require
    [cljs-time.core :as t]
    [clojure.string :as string]
    [district0x.ui.components.input :refer [token-input]]
    [district0x.ui.components.misc :as d0x-misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code]]
    [name-bazaar.shared.utils :refer [name-label emergency-state-new-owner]]
    [name-bazaar.ui.components.offering.auction-finalize-button :refer [auction-finalize-button]]
    [name-bazaar.ui.utils :refer [namehash sha3 path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn auction-bid-info []
  (let [now (subscribe [:now])]
    (fn [{:keys [:offering]}]
      (let [{:keys [:offering/address :auction-offering/end-time]} offering
            offering-status @(subscribe [:offering/status address])
            active-address-winning? @(subscribe [:auction-offering/active-address-winning-bidder? address])
            pending-returns (or @(subscribe [:auction-offering/active-address-pending-returns address]) 0)
            min-bid @(subscribe [:auction-offering/min-bid address])
            active? (= offering-status :offering.status/active)]
        (when (or active? (pos? pending-returns))
          [:div.description
           (when (and active-address-winning? active?)
             [:div.description.success
              (if (t/before? @now end-time)
                "Your bid is winning this auction!"
                "Your bid has won this auction!")])

           (when (pos? pending-returns)
             [:div
              "You have pending returns: " (format-eth-with-code pending-returns)
              " ("
              (if-not @(subscribe [:auction-offering.withdraw/tx-pending? address])
                [:a
                 {:on-click #(dispatch [:auction-offering/withdraw {:offering/address address}])}
                 "withdraw"]
                "withdrawing...")
              ")."])

           (when active?
             (let [min-bid-str (format-eth-with-code min-bid)]
               (cond
                 (pos? pending-returns)
                 [:div "For your next bid it's enough for you to send " min-bid-str " to beat the highest bid."]

                 active-address-winning?
                 [:div "You can raise your winning bid by sending new one and the value of old bid will be returned to you."
                  " You'd need to send at least " min-bid-str "."]

                 :else
                 [:div "You need to send at least " min-bid-str " to become the higest bidder."])))

           (when active?
             [:div "To place a bid, you can send Ether directly into the " [d0x-misc/etherscan-link
                                                                            {:address address}
                                                                            "offering address"]
              " or you can use the form below."])])))))
(defn non-valid-name-warning [props]
  [:div.description.warning
   [:b "WARNING: "] "Offered name is not compatible with UTS46 normalisation, "
   "therefore buying or bidding is disabled."])

(defn non-ascii-characters-warning [props]
  [:div.description.warning
   [:b "WARNING: "] "This name contains non-latin characters. Some of these characters may look same or "
   "very similar to their standard latin counterparts. If you're unsure what this means, please contact "
   "our team."])

(defn missing-ownership-warning []
  (let [active-address (subscribe [:district0x/active-address])]
    (fn [{:keys [:offering]}]
      (let [{:keys [:offering/original-owner]} offering]
        [:div.description.warning
         [:b "WARNING: "] (if (= @active-address original-owner)
                            "You"
                            "Original owner")
         " haven't transferred ownership of this name into the offering contract. "
         "Buying or bidding is not possible until the offering contract has ownership."]))))

(defn sub-level-name-warning [{:keys [:offering]}]
  (let [{:keys [:offering/name]} offering
        parent-name (subs (string/replace name (name-label name) "") 1)]
    [:div.description.warning
     [:b "WARNING:"] " This is" [:b " not top level name"] ". Beware, owner of " parent-name " will be always able to take this "
     "name back. Buy only when you trust the owner of " parent-name "."]))

(defn emergency-cancel-info []
  [:div.description.warning
   "This offering was cancelled by Name Bazaar emergency wallet, because of potential security risks. "
   "The contract won't be usable anymore."])

(defn deleted-info []
  [:div.description.warning
   "This offering was deleted by original owner."])

(defn offering-middle-section [{:keys [:offering]}]
  (let [{:keys [:offering/address :offering/contains-non-ascii? :offering/auction? :offering/top-level-name?
                :offering/new-owner :offering/valid-name? :offering/normalized? :offering/unregistered?]} offering
        missing-ownership? @(subscribe [:offering/missing-ownership? address])
        active-address-owner? @(subscribe [:offering/active-address-original-owner? address])
        show-auction-bid-info? (and auction? (not active-address-owner?))
        name-not-valid? (not (and valid-name? normalized?))
        emergency-cancel? (= new-owner emergency-state-new-owner)]
    [ui/GridColumn
     {:text-align :center
      :computer 10
      :tablet 12
      :mobile 16
      :class "offering-middle-section"}
     (when name-not-valid?
       [non-valid-name-warning])

     (when (and missing-ownership? (not unregistered?))
       [missing-ownership-warning
        {:offering offering}])

     (when (not top-level-name?)
       [sub-level-name-warning
        {:offering offering}])

     (when contains-non-ascii?
       [non-ascii-characters-warning])

     (when show-auction-bid-info?
       [auction-bid-info
        {:offering offering}])

     (when emergency-cancel?
       [emergency-cancel-info])

     (when unregistered?
       [deleted-info])]))
