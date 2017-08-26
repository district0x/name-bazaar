(ns components.active-address-balance
  (:require
    [district0x.ui.utils :as d0x-ui-utils]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn active-address-balance []
  (fn [{:keys [:token :token-name :max-fraction-digits]
        :or {max-fraction-digits 2 token :eth}
        :as props}]
    (let [balance @(subscribe [:district0x/active-address-balance token])]
      (when balance
        [:div
         (dissoc props :token :token-name :max-fraction-digits)
         (d0x-ui-utils/to-locale-string balance max-fraction-digits)
         " "
         [:span {:style {:text-transform :uppercase}}
          (or token-name (name token))]]))))
