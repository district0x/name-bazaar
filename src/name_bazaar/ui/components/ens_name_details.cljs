(ns name-bazaar.ui.components.ens-name-details
  (:require
    [clojure.string :as string]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [path-with-query]]
    [name-bazaar.shared.constants :refer [supported-tld-length?]]
    [name-bazaar.shared.utils :refer [top-level-name? name-label]]
    [name-bazaar.ui.components.add-to-watched-names-button :refer [add-to-watched-names-button]]
    [name-bazaar.ui.components.ens-record.etherscan-link :refer [ens-record-etherscan-link]]
    [name-bazaar.ui.components.ens-record.general-info :refer [ens-record-general-info]]
    [name-bazaar.ui.components.registrar-registration.general-info :refer [registrar-registration-general-info]]
    [name-bazaar.ui.utils :refer [namehash path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn name-detail-link [{:keys [:ens.record/name]}]
  [:div
   [:a.no-decor
    {:href (path-for :route.ens-record/detail {:ens.record/name name})}
    "Open Name Detail"]])

(defn create-offering-link []
  (let [active-address (subscribe [:district0x/active-address])]
    (fn [{:keys [:ens.record/name]}]
      (let [{:keys [:ens.record/owner :ens.record/name]} @(subscribe [:ens/record (namehash name)])]
        (when (and @active-address
                   (= owner @active-address))
          [:div
           [:a.no-decor
            {:href (path-with-query (path-for :route.offerings/create) {:name name})}
            "Create Offering"]])))))

(defn ens-name-details [{:keys [:ens.record/name
                                :show-name-detail-link?] :as props}]
  [:div
   [:div.grid.layout-grid.submit-footer.ens-name-detail

    [:div.general-info
     [ens-record-general-info
      {:ens.record/name name}]
     (when (top-level-name? name)
       [registrar-registration-general-info
        {:ens.record/name name}])]
    [:div.description.links-section
     (when show-name-detail-link?
       [name-detail-link
        {:ens.record/name name}])
     [ens-record-etherscan-link
      {:ens.record/name name}]
     [add-to-watched-names-button
      {:ens.record/name name}]
     [create-offering-link
      {:ens.record/name name}]]]])
