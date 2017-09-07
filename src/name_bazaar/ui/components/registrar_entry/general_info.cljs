(ns name-bazaar.ui.components.registrar-entry.general-info
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.shared.utils :as d0x-shared-utils :refer [empty-address?]]
    [district0x.ui.utils :refer [format-local-datetime format-eth-with-code]]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col etherscan-link]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [namehash name->label-hash registrar-entry-state->text]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn registrar-entry-general-info [{:keys [:ens.record/name] :as props}]
  (let [{:keys [:registrar.entry/state :registrar.entry.deed/address
                :registrar.entry/registration-date :registrar.entry.deed/value
                :registrar.entry.deed/address]}
        @(subscribe [:registrar/entry (name->label-hash name)])]
    [:div
     (r/merge-props
       {}
       (dissoc props :ens.record/name))
     [:div
      {:style styles/name-general-info-headline}
      "Registrar Information"]
     [:div "Status: " (registrar-entry-state->text state)]
     [:div
      {:style styles/text-overflow-ellipsis}
      "Registration Date: " (format-local-datetime registration-date)]
     [:div
      {:style styles/text-overflow-ellipsis}
      "Winning Deed: " [etherscan-link {:address address}]]
     [:div "Locked Value: " (format-eth-with-code (or value 0))]


     #_ [:div "Resolver: " (if-not (empty-address? resolver)
                          [etherscan-link {:address resolver}]
                          resolver)]]))
