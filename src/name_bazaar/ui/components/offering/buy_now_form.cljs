(ns name-bazaar.ui.components.offering.buy-now-form
  (:require
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col center-layout paper page]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]))

(defn buy-now-form [{:keys [:offering] :as props}]
  (let [{:keys [:offering/price :offering/address :offering/new-owner]} offering
        node-owner? @(subscribe [:offering/node-owner? address])]
    [row
     {:end "xs"
      :bottom "xs"
      :style (merge styles/full-height styles/full-width)}
     [raised-transaction-button
      {:primary true
       :label "Buy"
       :pending? @(subscribe [:buy-now-offering/buy-tx-pending? address])
       :pending-label "Buying..."
       :disabled (not node-owner?)
       :on-click #(dispatch [:buy-now-offering/buy {:offering/address address
                                                    :offering/price price}])}]]))