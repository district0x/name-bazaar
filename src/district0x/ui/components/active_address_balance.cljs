(ns district0x.ui.components.active-address-balance
  (:require
    [district0x.ui.utils :refer [to-locale-string]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn active-address-balance []
  (fn [{:keys [:token :token-name :max-fraction-digits]
        :or {max-fraction-digits 2 token :eth token-name "ETH"}
        :as props}]
    (let [balance @(subscribe [:district0x/active-address-balance token])]
      (when balance
        [:div.active-address-balance
         (dissoc props :token :token-name :max-fraction-digits)
         (to-locale-string balance max-fraction-digits)
         " "
         [:span (or token-name (name token))]]))))
