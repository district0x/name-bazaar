(ns name-bazaar.ui.components.offering.auction-finalize-button
  (:require
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]))

(defn auction-finalize-button []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:offering]}]
      (let [{:keys [:offering/address]} offering]
        [raised-transaction-button
         {:primary true
          :full-width @xs?
          :label "Finalize"
          :pending-label "Finalizing..."
          :pending? @(subscribe [:auction-offering.finalize/tx-pending? address])
          :style styles/margin-left-gutter-mini
          :on-click #(dispatch [:auction-offering/finalize {:offering/address address
                                                            :auction-offering/transfer-price? true}])}]))))
