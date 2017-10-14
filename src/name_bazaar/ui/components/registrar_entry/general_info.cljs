(ns name-bazaar.ui.components.registrar-entry.general-info
  (:require
    [district0x.shared.utils :refer [zero-address?]]
    [district0x.ui.components.misc :refer [etherscan-link]]
    [district0x.ui.utils :refer [format-local-datetime format-eth-with-code]]
    [name-bazaar.ui.utils :refer [name->label-hash registrar-entry-state->text]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))


(defn registrar-entry-general-info [{:keys [:ens.record/name] :as props}]
  (let [{:keys [:registrar.entry/state :registrar.entry.deed/address
                :registrar.entry/registration-date :registrar.entry.deed/value
                :registrar.entry.deed/address]}
        @(subscribe [:registrar/entry (name->label-hash name)])]
    [:div.description
     (dissoc props :ens.record/name)
     [:div [:b "Registrar Information"]]
     [:div "Status: " (registrar-entry-state->text state)]
     [:div.ellipsis
      "Registration Date: " (if registration-date
                              (format-local-datetime registration-date)
                              "none")]
     [:div.ellipsis
      "Winning Deed: " (if (zero-address? address)
                         "none"
                         [etherscan-link {:address address}])]
     [:div "Locked Value: " (format-eth-with-code (or value 0))]]))
