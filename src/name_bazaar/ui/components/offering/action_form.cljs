(ns name-bazaar.ui.components.offering.action-form
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils :refer [epoch->long empty-address?]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.text-field :refer [ether-field-with-currency]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code truncate]]
    [name-bazaar.shared.utils :refer [calculate-min-bid name-label emergency-state-new-owner]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.components.offering.auction-finalize-button :refer [auction-finalize-button]]
    [name-bazaar.ui.components.offering.auction-form :refer [auction-form]]
    [name-bazaar.ui.components.offering.buy-now-form :refer [buy-now-form]]
    [name-bazaar.ui.components.offering.general-info :refer [offering-general-info]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [namehash sha3 etherscan-ens-url path-for offering-type->text]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn new-owner-info [props]
  (let [active-address (subscribe [:district0x/active-address])
        my-addresses (subscribe [:district0x/my-addresses])]
    (fn [{:keys [:offering/new-owner]}]
      [:div
       (r/merge-props
         {:style styles/offering-list-item-new-owner-info}
         (dissoc props :offering/new-owner))
       (cond
         (= new-owner emergency-state-new-owner)
         "This offering was cancelled by NameBazaar due to emergency."

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

(defn transfer-ownership-button []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering] :as props}]
      (let [{:keys [:offering/address :offering/name :offering/top-level-name?]} offering
            label (name-label name)
            label-hash (sha3 label)
            node (namehash name)
            active-address-ens-owner? @(subscribe [:ens.record/active-address-owner? node])
            active-address-deed-owner? @(subscribe [:registrar.entry.deed/active-address-owner? label-hash])
            [transfer-event pending-sub] (if top-level-name?
                                           [[:registrar/transfer {:ens.record/label label :ens.record/owner address}]
                                            [:registrar.transfer/tx-pending? label]]
                                           [[:ens/set-owner {:ens.record/name name :ens.record/owner address}]
                                            [:ens.set-owner/tx-pending? node]])]
        [raised-transaction-button
         (r/merge-props
           {:secondary true
            :full-width @xs?
            :label "Transfer Ownership"
            :pending-label "Transferring..."
            :pending? @(subscribe pending-sub)
            :disabled (or (and top-level-name?
                               (or (not active-address-ens-owner?)
                                   (not active-address-deed-owner?)))
                          (and (not top-level-name?)
                               (not active-address-ens-owner?)))
            :on-click #(dispatch transfer-event)}
           (dissoc props :offering))]))))

(defn reclaim-ownership-button []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering]}]
      (let [{:keys [:offering/address]} offering]
        [raised-transaction-button
         {:secondary true
          :full-width @xs?
          :label "Reclaim Ownership"
          :pending-label "Reclaiming..."
          :pending? @(subscribe [:offering.reclaim-ownership/tx-pending? address])
          :style styles/margin-left-gutter-mini
          :on-click #(dispatch [:offering/reclaim-ownership {:offering/address address}])}]))))

(defn original-owner-form []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering]}]
      (let [{:keys [:offering/address :offering/buy-now? :auction-offering/bid-count]} offering
            needs-transfer? (false? @(subscribe [:offering/node-owner? address]))
            offering-status @(subscribe [:offering/status address])
            editable? (or buy-now? (zero? bid-count))]
        [row
         {:end "xs"
          :bottom "xs"
          :style (merge styles/full-height
                        styles/full-width)}
         (when needs-transfer?
           [transfer-ownership-button
            {:offering offering}])
         (when (and (not needs-transfer?)
                    editable?)
           [reclaim-ownership-button
            {:offering offering}])
         (when (and (not editable?)
                    (= offering-status :offering.status/auction-ended))
           [auction-finalize-button
            {:offering offering}])
         (when editable?
           [ui/raised-button
            {:primary true
             :full-width @xs?
             :label "Edit"
             :style styles/margin-left-gutter-mini
             :href (path-for :route.offerings/edit {:offering/address address})}])]))))

(defn action-form [{:keys [:offering]}]
  (let [{:keys [:offering/address :offering/new-owner :offering/auction? :auction-offering/bid-count]} offering]
    (cond
      new-owner
      [new-owner-info {:offering/new-owner new-owner}]

      @(subscribe [:offering/active-address-original-owner? address])
      [original-owner-form {:offering offering}]

      auction?
      [auction-form {:offering offering}]

      :else
      [buy-now-form {:offering offering}])))
