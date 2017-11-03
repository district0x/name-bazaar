(ns name-bazaar.ui.components.offering.bottom-section
  (:require
    [district0x.ui.components.input :refer [token-input]]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [name-bazaar.ui.components.offering.auction-finalize-button :refer [auction-finalize-button]]
    [name-bazaar.ui.utils :refer [namehash sha3 path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn transfer-ownership-button [{:keys [:offering] :as props}]
  (let [{:keys [:offering/address :offering/name :offering/top-level-name? :offering/label
                :offering/label-hash :offering/node]} offering
        active-address-ens-owner? @(subscribe [:ens.record/active-address-owner? node])
        active-address-deed-owner? @(subscribe [:registrar.entry.deed/active-address-owner? label-hash])
        [transfer-event pending-sub] (if top-level-name?
                                       [[:registrar/transfer {:ens.record/label label :ens.record/owner address}]
                                        [:registrar.transfer/tx-pending? label]]
                                       [[:ens/set-owner {:ens.record/name name :ens.record/owner address}]
                                        [:ens.set-owner/tx-pending? node]])]
    [transaction-button
     (r/merge-props
       {:secondary true
        :pending-text "Transferring..."
        :pending? @(subscribe pending-sub)
        :disabled (or (and top-level-name?
                           (or (not active-address-ens-owner?)
                               (not active-address-deed-owner?)))
                      (and (not top-level-name?)
                           (not active-address-ens-owner?)))
        :on-click #(dispatch transfer-event)}
       (dissoc props :offering))
     "Transfer Ownership"]))

(defn delete-offering-button [{:keys [:offering/address :offering/type] :as props}]
  [transaction-button
   {:color :pink
    :pending-text "Deleting..."
    :pending? @(subscribe [:offering.unregister/tx-pending? address type])
    :on-click #(dispatch [:offering/unregister {:offering/address address}])}
   "Delete"])

(defn section-for-original-owner [{:keys [:offering]}]
  (let [{:keys [:offering/address :offering/buy-now? :auction-offering/bid-count :offering/unregistered? :offering/type]} offering
        needs-transfer? (false? @(subscribe [:offering/node-owner? address]))
        can-delete? needs-transfer?
        offering-status @(subscribe [:offering/status address])
        editable? (or buy-now? (zero? bid-count))
        finalizable? (and (not editable?)
                          (= offering-status :offering.status/auction-ended))]
    (when (and (not (contains? #{:offering.status/finalized :offering.status/emergency} offering-status))
               (not unregistered?))
      [:div.bottom-section-buttons
       (when needs-transfer?
         [transfer-ownership-button
          {:offering offering}])
       (when (and (not needs-transfer?)
                  (not finalizable?))
         [transaction-button
          {:color :pink
           :pending-text "Reclaiming..."
           :disabled (not editable?)
           :pending? @(subscribe [:offering.reclaim-ownership/tx-pending? address])
           :on-click #(dispatch [:offering/reclaim-ownership {:offering/address address}])}
          "Reclaim Ownership"])
       (when finalizable?
         [auction-finalize-button
          {:offering offering}])
       (when-not finalizable?
         [ui/Button
          {:as "a"
           :disabled (not editable?)
           :color "purple"
           :href (path-for :route.offerings/edit {:offering/address address})}
          "Edit"])
       (when can-delete?
         [delete-offering-button {:offering/address address :offering/type type}])])))

(defn- offering-buyable? [offering-status]
  (not (contains? #{:offering.status/auction-ended :offering.status/finalized :offering.status/emergency} offering-status)))

(defn section-for-auction-bidder []
  (let [bid-value (r/atom nil)]
    (fn [{:keys [:offering] :as props}]
      (let [{:keys [:offering/address :offering/price :auction-offering/min-bid-increase
                    :auction-offering/bid-count :offering/valid-name? :offering/normalized?]} offering
            invalid-name? (not (and valid-name? normalized?))
            offering-status @(subscribe [:offering/status address])
            pending-returns (or @(subscribe [:auction-offering/active-address-pending-returns address]) 0)
            active-address-winning? @(subscribe [:auction-offering/active-address-winning-bidder? address])
            min-bid @(subscribe [:auction-offering/min-bid address])]
        (cond
          (and active-address-winning?
               (= offering-status :offering.status/auction-ended))
          [:div.bottom-section-buttons
           [auction-finalize-button
            {:offering offering}]]

          (offering-buyable? offering-status)
          [ui/GridColumn
           {:widescreen 6
            :large-screen 7
            :computer 8
            :tablet 8
            :mobile 14
            :text-align :center}
           [:div.bid-section
            [:div.header "Your Bid"]
            [:div.input-section
             [token-input
              {:value (or @bid-value min-bid)
               :disabled invalid-name?
               :on-change #(reset! bid-value (aget %2 "value"))
               :fluid true}
              "Your bid"]
             [transaction-button
              {:primary true
               :pending? @(subscribe [:auction-offering.bid/tx-pending? address])
               :pending-text "Bidding..."
               :disabled (or
                           invalid-name?
                           (not (>= (or @bid-value min-bid) min-bid))
                           (= offering-status :offering.status/missing-ownership))
               :on-click #(dispatch [:auction-offering/bid {:offering/address address
                                                            :offering/price (or @bid-value min-bid)}])}
              "Bid Now"]]]])))))

(defn section-for-buy-now-buyer [{:keys [:offering] :as props}]
  (let [{:keys [:offering/price :offering/address :offering/new-owner :offering/valid-name?
                :offering/normalized?]} offering
        invalid-name? (not (and valid-name? normalized?))
        offering-status @(subscribe [:offering/status address])]
    (when (offering-buyable? offering-status)
      [:div
       [transaction-button
        {:primary true
         :pending? @(subscribe [:buy-now-offering.buy/tx-pending? address])
         :pending-text "Buying..."
         :disabled (or
                     invalid-name?
                     (= offering-status :offering.status/missing-ownership))
         :on-click #(dispatch [:buy-now-offering/buy {:offering/address address
                                                      :offering/price price}])}
        "Buy"]])))

(defn offering-bottom-section [{:keys [:offering]}]
  (let [{:keys [:offering/address :offering/new-owner :offering/auction? :auction-offering/bid-count]} offering]
    (cond

      @(subscribe [:offering/active-address-original-owner? address])
      [section-for-original-owner {:offering offering}]


      auction?
      [section-for-auction-bidder {:offering offering}]

      :else
      [section-for-buy-now-buyer {:offering offering}])))
