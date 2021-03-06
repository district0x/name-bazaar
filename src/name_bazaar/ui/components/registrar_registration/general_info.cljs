(ns name-bazaar.ui.components.registrar-registration.general-info
  (:require
    [district0x.shared.utils :refer [zero-address?]]
    [district0x.ui.components.misc :refer [etherscan-link]]
    [district0x.ui.utils :refer [format-local-datetime format-eth-with-code]]
    [name-bazaar.ui.utils :refer [name->label-hash]]
    [name-bazaar.shared.utils :refer [name-label top-level-name?]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn registrar-registration-general-info [{:keys [:ens.record/name] :as props}]
  (let [{:keys [:eth-registrar.registration/available
                :eth-registrar.registration/expiration-date
                :eth-registrar.registration/owner]}
        @(subscribe [:eth-registrar/registration (name->label-hash name)])]
    [:div.description
     (-> props (dissoc :ens.record/name)
         (dissoc :eth-registrar.registration/owner))
     [:div [:b "Registrar Information"]]
     [:div "Registration Available: " (case available true "Yes" false "No" "No Information")]
     [:div.ellipsis
      "Expiration Date: " (if expiration-date
                              (format-local-datetime expiration-date)
                              "none")]
     [:div.ellipsis
      "Registrant: " (if (zero-address? owner)
                      "none"
                      [etherscan-link {:address owner}])]]))
