(ns name-bazaar.ui.components.ens-name-details
  (:require
    [clojure.string :as string]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [name-bazaar.ui.components.add-to-watched-names-button :refer [add-to-watched-names-button]]
    [name-bazaar.ui.components.ens-record.etherscan-link :refer [ens-record-etherscan-link]]
    [name-bazaar.ui.components.ens-record.general-info :refer [ens-record-general-info]]
    [name-bazaar.ui.components.registrar-entry.general-info :refer [registrar-entry-general-info]]
    [name-bazaar.ui.utils :refer [namehash path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn add-request-button [{:keys [:ens.record/name]}]
  (let [has-requested? @(subscribe [:offering-request/active-address-has-requested? (namehash name)])
        tx-pending? @(subscribe [:offering-requests.add-request/tx-pending? name])]
    [transaction-button
     {:primary true
      :pending? tx-pending?
      :pending-text "Requesting..."
      :disabled has-requested?
      :on-click #(dispatch [:offering-requests/add-request {:ens.record/name name}])}
     (if has-requested? "Requested" "Request")]))

(defn name-detail-link [{:keys [:ens.record/name]}]
  [:div
   [:a.no-decor
    {:href (path-for :route.ens-record/detail {:ens.record/name name})}
    "Open Name Detail"]])

(defn ens-name-details [{:keys [:ens.record/name :show-name-detail-link?] :as props}]
  [ui/Grid
   {:class "layout-grid submit-footer ens-name-detail"
    :celled :internally}
   [ui/GridRow
    [ui/GridColumn
     {:computer 8
      :tablet 8
      :mobile 16}
     [ens-record-general-info
      {:ens.record/name name}]
     (when (top-level-name? name)
       [registrar-entry-general-info
        {:ens.record/name name}])]
    [ui/GridColumn
     {:computer 8
      :tablet 8
      :text-align :right
      :mobile 16}
     [:div.description.links-section
      (when show-name-detail-link?
        [name-detail-link
         {:ens.record/name name}])
      [ens-record-etherscan-link
       {:ens.record/name name}]
      [add-to-watched-names-button
       {:ens.record/name name}]]]]
   [ui/GridRow
    {:centered true}
    [:div
     [add-request-button
      {:ens.record/name name}]]]])
