(ns name-bazaar.ui.components.ens-record.general-info
  (:require
    [district0x.shared.utils :refer [empty-address? zero-address?]]
    [district0x.ui.components.misc :refer [etherscan-link]]
    [name-bazaar.ui.utils :refer [namehash path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn ens-record-general-info [{:keys [:ens.record/name] :as props}]
  (let [{:keys [:ens.record/owner :ens.record/resolver]} @(subscribe [:ens/record (namehash name)])]
    [:div.ens-record-general-info.description
     (dissoc props :ens.record/name)
     [:div [:b "ENS Information"]]
     [:div.ellipsis
      "Owner: " (cond
                  (not (empty-address? owner))
                  [:a
                   {:href (path-for :route.user/offerings {:user/address owner})}
                   owner]

                  (zero-address? owner)
                  "none"

                  :else "")]
     [:div.ellipsis
      "Resolver: " (if (zero-address? resolver)
                     "none"
                     [etherscan-link {:address resolver}])]]))
