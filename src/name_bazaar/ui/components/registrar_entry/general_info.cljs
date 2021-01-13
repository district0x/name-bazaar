(ns name-bazaar.ui.components.registrar-entry.general-info
  (:require
    [district0x.shared.utils :refer [zero-address?]]
    [district0x.ui.components.misc :refer [etherscan-link]]
    [district0x.ui.utils :refer [format-local-datetime format-eth-with-code]]
    [name-bazaar.ui.utils :refer [name->label-hash]]
    [name-bazaar.shared.utils :refer [name-label top-level-name?]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn registrar-entry-general-info [{:keys [:ens.record/name :name-bazaar-registrar.entry/state-text] :as props}]
  (let [{:keys [:name-bazaar-registrar.entry.deed/address
                :name-bazaar-registrar.entry/registration-date
                :name-bazaar-registrar.entry.deed/value
                :name-bazaar-registrar.entry.deed/address]}
        @(subscribe [:name-bazaar-registrar/entry (name->label-hash name)])]
    [:div.description
     (-> props (dissoc :ens.record/name)
         (dissoc :name-bazaar-registrar.entry/state-text))
     [:div [:b "Registrar Information"]]
     [:div "Status: " state-text]
     [:div.ellipsis
      "Registration Date: " (if registration-date
                              (format-local-datetime registration-date)
                              "none")]
     [:div.ellipsis
      "Winning Deed: " (if (zero-address? address)
                         "none"
                         [etherscan-link {:address address}])]
     [:div "Locked Value: " (format-eth-with-code (or value 0))]]))
