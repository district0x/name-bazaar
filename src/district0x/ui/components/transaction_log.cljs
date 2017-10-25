(ns district0x.ui.components.transaction-log
  (:require
    [district0x.shared.utils :refer [wei->eth]]
    [district0x.ui.components.misc :refer [etherscan-link]]
    [district0x.ui.location-fx :as location-fx]
    [district0x.ui.utils :as d0x-ui-utils :refer [time-ago to-locale-string truncate format-eth-with-code]]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn main-title [props]
  [:div.title
   props
   "TRANSACTION LOG"])

(defn settings []
  (let [settings (subscribe [:district0x.transaction-log/settings])]
    (fn [{:keys [:container-props :from-active-address-only-toggle-props]}]
      (let [{:keys [:from-active-address-only?]} @settings]
        [:div.settings
         container-props
         [ui/Checkbox
          (r/merge-props
            {:toggle true
             :label "Show transactions from active address only."
             :on-change #(dispatch [:district0x.transaction-log.settings/set :from-active-address-only? (aget %2 "checked")])
             :checked from-active-address-only?}
            from-active-address-only-toggle-props)]]))))

(defn transaction-time-ago [{{:keys [:created-on]} :transaction}]
  [:div.transaction-time-ago
   "Sent " (time-ago created-on)])

(defn transaction-gas [{{:keys [:status :gas :gas-used :gas-used-cost-usd]} :transaction}]
  [:div.transaction-gas
   (cond
     (contains? #{:tx.status/success :tx.status/failure} status)
     (str "Gas used: " (to-locale-string gas-used 0) (when gas-used-cost-usd
                                                       (str " ($" (to-locale-string gas-used-cost-usd 2) ")")))

     gas
     (str "Gas limit: " gas)

     :else
     (str "Gas used: ..."))])

(defn transaction-from [{{:keys [:tx-opts]} :transaction}]
  [:div.transaction-sender
   "From: " [etherscan-link
             {:address (:from tx-opts)}
             (truncate (:from tx-opts) 20)]])

(defn transaction-id [{{:keys [:hash]} :transaction}]
  [:div.transaction-id
   "Tx ID: " [etherscan-link
              {:address hash
               :transaction? true}
              (truncate hash 20)]])

(defn transaction-value [{{:keys [:value]} :transaction}]
  [:div.transaction-value
   (format-eth-with-code (wei->eth value))])

(defn transaction-name [{:keys [:transaction] :as props}]
  (let [{:keys [:contract-key :contract-method :name]} transaction]
    [:div.transaction-name
     (dissoc props :transaction)
     name]))

(def transaction-status->text
  {:tx.status/success "Completed"
   :tx.status/failure "Failed"
   :tx.status/pending "Pending"
   :tx.status/not-loaded "Loading"})

(defn transaction-status [{{:keys [:status]} :transaction}]
  (let [status-text (transaction-status->text status)]
    [:div.transaction-status
     {:class (name status)}
     [:i.icon]
     [:div.transaction-status-text status-text]]))

(defn transaction [{:keys [:transaction :container-props :border-bottom-style]}]
  (let [{:keys [:hash :contract-key :contract-method :result-href :highlighted?]} transaction]
    [:div.transaction
     {:class (when highlighted? "highlighted")
      :on-click (fn [e]
                  (when (and (not (instance? js/HTMLAnchorElement (aget e "target")))
                             result-href)
                    (if (d0x-ui-utils/hashroutes?)
                      (location-fx/set-location-hash! result-href)
                      (location-fx/set-history! result-href))
                    (dispatch [:district0x.transaction-log/set-open false])))}
     [transaction-name {:transaction transaction}]
     [:div.transaction-content
      [:div.left-section
       [transaction-time-ago {:transaction transaction}]
       [transaction-gas {:transaction transaction}]
       [transaction-from {:transaction transaction}]
       [transaction-id {:transaction transaction}]]
      [:div.right-section
       [transaction-status {:transaction transaction}]
       [transaction-value {:transaction transaction}]]]]))

(defn no-transactions []
  [:div.no-transactions "You haven't made any transactions yet."])

(defn transactions []
  (let [tx-log (subscribe [:district0x.transaction-log/transactions])]
    (fn [{:keys [:container-props] :as props}]
      (let [tx-log-items @tx-log]
        (if (seq tx-log-items)
          [:div.transactions
           container-props
           (for [{:keys [:hash] :as tx} tx-log-items]
             [transaction
              {:key hash
               :transaction tx
               :last? (= hash (:hash (last tx-log-items)))}])]
          [no-transactions])))))

(defn transaction-log [props]
  (let [active-address (subscribe [:district0x/active-address])
        open? (subscribe [:district0x.transaction-log/open?])]
    (fn [props]
      (when @active-address
        [:div.transaction-log
         (r/merge-props
           {:class (when @open? "open")}
           (dissoc props :title-props :settings-props :items-props))
         [main-title (:title-props props)]
         [settings (:settings-props props)]
         [transactions (:items-props props)]]))))
