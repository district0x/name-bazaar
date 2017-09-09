(ns name-bazaar.ui.components.ens-name-details
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [name-bazaar.ui.components.add-to-watched-names-button :refer [add-to-watched-names-button]]
    [name-bazaar.ui.components.ens-record.etherscan-link :refer [ens-record-etherscan-link]]
    [name-bazaar.ui.components.ens-record.general-info :refer [ens-record-general-info]]
    [name-bazaar.ui.components.misc :refer [a]]
    [name-bazaar.ui.components.registrar-entry.general-info :refer [registrar-entry-general-info]]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [namehash]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn add-request-button []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:ens.record/name]}]
      (let [has-requested? @(subscribe [:offering-request/active-address-has-requested? (namehash name)])
            tx-pending? @(subscribe [:offering-requests.add-request/tx-pending? name])]
        [raised-transaction-button
         {:primary true
          :label (if has-requested? "Requested" "Request")
          :pending? tx-pending?
          :pending-label "Requesting..."
          :disabled has-requested?
          :full-width @xs?
          :on-click #(dispatch [:offering-requests/add-request {:ens.record/name name}])}]))))

(defn name-detail-link [{:keys [:ens.record/name]}]
  [:div
   [a
    {:route :route.ens-record/detail
     :route-params {:ens.record/name name}
     :style styles/text-decor-none}
    "Open Name Detail"]])

(defn ens-name-details []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:ens.record/name :show-name-detail-link?] :as props}]
      [row-with-cols
       (r/merge-props
         {:style (styles/search-results-list-item-body @xs?)}
         (dissoc props :ens.record/name :show-name-detail-link?))
       [col
        {:xs 12 :sm 8 :style styles/margin-bottom-gutter-mini}
        [ens-record-general-info
         {:ens.record/name name
          :style styles/margin-bottom-gutter-mini}]
        (when (top-level-name? name)
          [registrar-entry-general-info
           {:ens.record/name name}])]
       [col
        {:xs 12 :sm 4 :style (styles/list-item-body-links-container @xs?)}
        (when show-name-detail-link?
          [name-detail-link
           {:ens.record/name name}])
        [ens-record-etherscan-link
         {:ens.record/name name}]
        [add-to-watched-names-button
         {:ens.record/name name}]]
       [col
        {:xs 12}
        [row
         {:end "xs"
          :bottom "xs"
          :style (merge styles/full-height styles/full-width)}
         [add-request-button
          {:ens.record/name name}]]]])))
