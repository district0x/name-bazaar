(ns name-bazaar.ui.components.offering.auction-finalize-button
  (:require
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [re-frame.core :refer [subscribe dispatch]]))

(defn auction-finalize-button [{:keys [:offering]}]
  (let [{:keys [:offering/address]} offering]
    [transaction-button
     {:primary true
      :pending-text "Finalizing..."
      :pending? @(subscribe [:auction-offering.finalize/tx-pending? address])
      :on-click #(dispatch [:auction-offering/finalize {:offering/address address}])}
     "Finalize"]))
