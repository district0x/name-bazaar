(ns district0x.ui.components.transaction-log
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.components.misc :as d0x-misc :refer [row row-with-cols col]]
    [district0x.ui.utils :as d0x-ui-utils :refer [create-icon]]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def bell-icon (create-icon "M14,20A2,2 0 0,1 12,22A2,2 0 0,1 10,20H14M12,2A1,1 0 0,1 13,3V4.08C15.84,4.56 18,7.03 18,10V16L21,19H3L6,16V10C6,7.03 8.16,4.56 11,4.08V3A1,1 0 0,1 12,2Z"))
(def check-circle-icon (create-icon "M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M11,16.5L18,9.5L16.59,8.09L11,13.67L7.91,10.59L6.5,12L11,16.5Z"))
(def alert-icon (create-icon "M13,14H11V10H13M13,18H11V16H13M1,21H23L12,2L1,21Z"))
(def timer-icon (create-icon "M12,20A7,7 0 0,1 5,13A7,7 0 0,1 12,6A7,7 0 0,1 19,13A7,7 0 0,1 12,20M19.03,7.39L20.45,5.97C20,5.46 19.55,5 19.04,4.56L17.62,6C16.07,4.74 14.12,4 12,4A9,9 0 0,0 3,13A9,9 0 0,0 12,22C17,22 21,17.97 21,13C21,10.88 20.26,8.93 19.03,7.39M11,14H13V8H11M15,1H9V3H15V1Z"))

(def tx-value-style {:line-height "18px" :font-size "18px"})
(def no-txs-row-style {:text-align "center" :width "100%" :font-size "12px" :color "#333" :height 300})
(def icon-menu-style {:padding-left 0})
(def icon-menu-list-style {:padding-top 10 :padding-left 0})
(def tx-info-line-style {:line-height "14px" :font-size "11.5px" :color "#333"})
(def tx-info-link-style {:text-decoration :underline})
(def tx-status-row-style {:line-height "10px" :font-size "10px"})
(def tx-status-icon-style {:width 14 :margin-left 3})
(def tx-name-style {:line-height "25px" :max-width 310 :overflow :hidden :text-overflow :ellipsis})
(def title-style {:text-align "center" :font-size 12 :font-weight "bold"})
(def toggle-settings-style {:margin-top 10 :padding-left 15})
(def tx-item-border-bottom-style {:border-bottom "0.5px solid #eee"})
(def tx-items-container-style {:margin-top 10 :min-height 300})

(defn main-title [props]
  [:div
   (r/merge-props {:style title-style} props)
   "TRANSACTION LOG"])

(defn settings []
  (let [settings (subscribe [:district0x/transaction-log-settings])]
    (fn [{:keys [:container-props :from-active-address-only-toggle-props]}]
      (let [{:keys [:from-active-address-only?]} @settings]
        [:div
         (r/merge-props
           {:style toggle-settings-style}
           container-props)
         [ui/toggle
          (r/merge-props
            {:label "Show transactions from active address only"
             :label-position "right"
             :label-style {:font-size 12}
             :on-toggle #(dispatch [:district0x.transaction-log-settings/set :from-active-address-only? %2])
             :toggled from-active-address-only?}
            from-active-address-only-toggle-props)]]))))

(defn transaction-time-ago [{{:keys [:created-on]} :transaction}]
  [:div {:style tx-info-line-style}
   "Sent " (d0x-ui-utils/time-ago created-on)])

(defn transaction-gas [{{:keys [:status :gas :gas-used :gas-used-cost]} :transaction}]
  [:div
   {:style tx-info-line-style}
   (if (contains? #{:tx.status/success :tx.status/failure} status)
     (str "Gas used: " (d0x-ui-utils/to-locale-string gas-used 0) (when gas-used-cost "($" gas-used-cost ")"))
     (str "Gas limit: " gas))])

(defn transaction-from [{{:keys [:tx-opts]} :transaction}]
  [:div {:style tx-info-line-style}
   "From: " [d0x-misc/etherscan-link
             {:address (:from tx-opts)
              :style tx-info-link-style}
             (d0x-ui-utils/truncate (:from tx-opts) 20)]])

(defn transaction-id [{{:keys [:hash]} :transaction}]
  [:div {:style tx-info-line-style}
   "Tx ID: " [d0x-misc/etherscan-link
              {:tx-hash hash
               :style tx-info-link-style}
              (d0x-ui-utils/truncate hash 20)]])

(defn transaction-value [{{:keys [:value]} :transaction}]
  [:div
   {:style tx-value-style}
   (d0x-ui-utils/format-eth-with-code (d0x-shared-utils/wei->eth value))])

(defn transaction-name [{:keys [:transaction] :as props}]
  (let [{:keys [:contract-key :contract-method :name]} transaction]
    [:div
     (r/merge-props
       {:style tx-name-style}
       (dissoc props :transaction))
     name]))

(def transaction-statuses
  {:tx.status/success ["Completed" check-circle-icon]
   :tx.status/failure ["Failed" alert-icon]
   :tx.status/pending ["Pending" timer-icon]
   :tx.status/not-loaded ["Loading" timer-icon]})

(defn transaction-status [{{:keys [:status]} :transaction}]
  (let [[status-text icon] (transaction-statuses status)]
    [row
     {:middle "xs"
      :end "xs"
      :style tx-status-row-style}
     status-text
     (icon {:style tx-status-icon-style})]))

(defn transaction [{:keys [:transaction :last? :container-props :border-bottom-style]}]
  (let [{:keys [:hash :contract-key :contract-method :result-href]} transaction]
    [ui/menu-item
     (r/merge-props
       {:style (when-not last?
                 (or tx-item-border-bottom-style border-bottom-style))
        :inner-div-style {:padding 10}
        :on-click (fn [e]
                    (when-not (and (instance? js/HTMLAnchorElement (aget e "target"))
                                   result-href)
                      (set! (.-hash js/location) result-href)))}
       container-props)
     [:div
      [transaction-name {:transaction transaction}]
      [row-with-cols
       [col
        {:xs 8}
        [transaction-time-ago {:transaction transaction}]
        [transaction-gas {:transaction transaction}]
        [transaction-from {:transaction transaction}]
        [transaction-id {:transaction transaction}]]
       [col
        {:xs 4
         :style {:text-align :right}}
        [row
         {:end "xs"
          :bottom "xs"
          :style {:height "100%"}}
         [:div
          [transaction-status {:transaction transaction}]
          [transaction-value {:transaction transaction}]]]
        ]]]]))

(defn no-transactions []
  [row
   {:middle "xs"
    :center "xs"
    :style no-txs-row-style}
   "You haven't made any transactions yet."])

(defn transactions []
  (let [tx-log (subscribe [:district0x/transaction-log])]
    (fn [{:keys [:container-props] :as props}]
      (let [tx-log-items @tx-log]
        [:div
         (r/merge-props
           {:style tx-items-container-style}
           container-props)
         (if (seq tx-log-items)
           (for [{:keys [:hash] :as tx} tx-log-items]
             [transaction
              {:key hash
               :transaction tx
               :last? (= hash (:hash (last tx-log-items)))}])
           [no-transactions])]))))

(defn transaction-log-layout [props & children]
  (let [[props [tx-log-title tx-log-settings tx-log-items]] (d0x-ui-utils/parse-props-children props children)]
    [ui/icon-menu
     (r/merge-props
       {:icon-button-element (r/as-element [ui/icon-button (bell-icon {:color "#FFF"})])
        :anchor-origin {:horizontal "right" :vertical "top"}
        :target-origin {:horizontal "right" :vertical "top"}
        :style icon-menu-style
        :list-style icon-menu-list-style
        :max-height 600}
       props)
     tx-log-title
     tx-log-settings
     tx-log-items]))

(defn transaction-log [props]
  (let [active-address (subscribe [:district0x/active-address])]
    (fn []
      (when @active-address
        [transaction-log-layout
         (:icon-menu-props props)
         [main-title (:title-props props)]
         [settings (:settings-props props)]
         [transactions (:items-props props)]]))))