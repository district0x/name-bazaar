(ns district0x.ui.subs
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [district0x.ui.utils :as d0x-ui-utils]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :district0x/db
  (fn [db [_ key]]
    (get db key)))

(reg-sub
  :district0x/my-addresses
  (fn [db _]
    (:my-addresses db)))

(reg-sub
  :district0x/active-address
  (fn [db _]
    (:active-address db)))

(reg-sub
  :district0x/can-submit-into-blockchain?
  (fn [db _]
    (boolean (and (or (d0x-ui-utils/provides-web3?) (:load-node-addresses? db))
                  (:active-address db)))))

(reg-sub
  :district0x/active-page
  (fn [db _]
    (:active-page db)))

(reg-sub
  :district0x/conversion-rates
  (fn [db]
    (:conversion-rates db)))

(reg-sub
  :district0x/smart-contracts
  (fn [db]
    (:smart-contracts db)))

(reg-sub
  :district0x/balances
  (fn [db _]
    (:balances db)))

(reg-sub
  :district0x/active-address-balance
  :<- [:district0x/active-address]
  :<- [:district0x/balances]
  (fn [[active-address balances] [_ token]]
    (get-in balances [active-address (or token :eth)])))

(reg-sub
  :district0x/form
  (fn [db [_ form-key]]
    (db form-key)))

(reg-sub
  :district0x/search-results
  (fn [db [_ search-results-key]]
    (db search-results-key)))

(reg-sub
  :district0x/contracts-not-found?
  (fn [db _]
    (:contracts-not-found? db)))

(reg-sub
  :district0x/blockchain-connection-error?
  (fn [db]
    (:district0x/blockchain-connection-error? db)))

(reg-sub
  :district0x/snackbar
  (fn [db]
    (:snackbar db)))

(reg-sub
  :district0x/dialog
  (fn [db]
    (:dialog db)))

(reg-sub
  :district0x/window-width-size
  (fn [db]
    (:window-width-size db)))

(reg-sub
  :district0x/window-lg-width?
  (fn [db]
    (= (:window-width-size db) 3)))

(reg-sub
  :district0x/window-md-lg-width?
  (fn [db]
    (>= (:window-width-size db) 2)))

(reg-sub
  :district0x/window-xs-width?
  (fn [db]
    (= (:window-width-size db) 0)))

(reg-sub
  :district0x/window-xs-sm-width?
  (fn [db]
    (<= (:window-width-size db) 1)))

(reg-sub
  :district0x/ui-disabled?
  (fn [db _]
    (:ui-disabled? db)))

(reg-sub
  :district0x/form-configs
  (fn [db]
    (:form-configs db)))

(reg-sub
  :district0x/transactions
  (fn [db]
    (:transactions db)))

(reg-sub
  :district0x/transaction-ids-chronological
  (fn [db]
    (:transaction-ids-chronological db)))

(reg-sub
  :district0x/transaction-ids-by-form
  (fn [db]
    (:transaction-ids-by-form db)))

(reg-sub
  :district0x/transaction-log
  :<- [:district0x/transactions]
  :<- [:district0x/transaction-ids-chronological]
  :<- [:district0x/form-configs]
  (fn [[transactions transaction-ids-chronological form-configs]]
    (->> transaction-ids-chronological
      (map transactions)
      (map #(assoc % :form-config (form-configs (:form-key %)))))))

(reg-sub
  :district0x/tx-pending?
  :<- [:district0x/active-address]
  :<- [:district0x/transactions]
  :<- [:district0x/transaction-ids-by-form]
  (fn [[active-address transactions transactions-by-form] [_ form-key form-id]]
    (-> (get-in transactions-by-form [form-key active-address form-id])
      first
      transactions
      :block-hash
      empty?)))





