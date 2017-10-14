(ns district0x.ui.subs
  (:require
    [cemerick.url :as url]
    [cljs-time.coerce :refer [from-long]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [district0x.ui.utils :as d0x-ui-utils :refer [to-locale-string]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :district0x/db
  (fn [db [_ key]]
    (get db key)))

(reg-sub
  :district0x/my-addresses
  (fn [db _]
    (:my-addresses db)))

(reg-sub
  :district0x.my-addresses/single-address?
  :<- [:district0x/my-addresses]
  (fn [my-addresses]
    (= (count my-addresses) 1)))

(reg-sub
  :district0x/active-address
  (fn [db _]
    (:active-address db)))

(reg-sub
  :district0x/can-make-transaction?
  (fn [db _]
    (boolean (and (or (d0x-ui-utils/provides-web3?) (:load-node-addresses? db))
                  (:active-address db)))))

(reg-sub
  :district0x/active-page
  (fn [db _]
    (:active-page db)))

(reg-sub
  :district0x/route-params
  :<- [:district0x/active-page]
  (fn [active-page]
    (:route-params active-page)))

(reg-sub
  :district0x/query-params
  :<- [:district0x/active-page]
  (fn [active-page]
    (:query-params active-page)))

(reg-sub
  :district0x/parsed-query-params
  :<- [:district0x/query-params]
  (fn [query-params [_ parsers]]
    ))

(reg-sub
  :district0x/query-string
  :<- [:district0x/query-params]
  (fn [query-params]
    (url/map->query query-params)))

(reg-sub
  :district0x/conversion-rates
  (fn [db]
    (:conversion-rates db)))

(reg-sub
  :district0x/conversion-rate
  :<- [:district0x/conversion-rates]
  (fn [conversion-rates [_ currency]]
    (get conversion-rates currency)))

(reg-sub
  :district0x/smart-contracts
  (fn [db]
    (:smart-contracts db)))

(reg-sub
  :district0x/balances
  (fn [db _]
    (:balances db)))

(reg-sub
  :district0x/menu-drawer-open?
  (fn [db _]
    (get-in db [:menu-drawer :open?])))

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
  :district0x/screen-size
  (fn [db]
    (:screen-size db)))

(reg-sub
  :district0x.screen-size/min-large-screen?
  :<- [:district0x/screen-size]
  (fn [screen-size]
    (>= screen-size 3)))

(reg-sub
  :district0x.screen-size/min-computer-screen?
  :<- [:district0x/screen-size]
  (fn [screen-size]
    (>= screen-size 2)))

(reg-sub
  :district0x.screen-size/min-computer?
  :<- [:district0x/screen-size]
  (fn [screen-size]
    (>= screen-size 2)))

(reg-sub
  :district0x.screen-size/mobile?
  :<- [:district0x/screen-size]
  (fn [screen-size]
    (= screen-size 0)))

(reg-sub
  :district0x.screen-size/max-tablet?
  :<- [:district0x/screen-size]
  (fn [screen-size]
    (<= screen-size 1)))

(reg-sub
  :district0x/ui-disabled?
  (fn [db _]
    (:ui-disabled? db)))

(reg-sub
  :district0x/transaction-log
  (fn [db]
    (:transaction-log db)))

(reg-sub
  :district0x.transaction-log/settings
  :<- [:district0x/transaction-log]
  (fn [transaction-log _]
    (get transaction-log :settings)))

(reg-sub
  :district0x.transaction-log/open?
  :<- [:district0x.transaction-log/settings]
  (fn [transaction-log-settings _]
    (get transaction-log-settings :open?)))

(reg-sub
  :district0x.transaction-log/transactions
  :<- [:district0x/transaction-log]
  :<- [:district0x/active-address]
  (fn [[transaction-log active-address]]
    (let [{:keys [:transactions :ids-chronological :settings]} transaction-log
          {:keys [:from-active-address-only? :highlighted-transaction]} settings]
      (cond->> ids-chronological
        true (map transactions)
        true (map #(update % :created-on from-long))
        highlighted-transaction (map (fn [tx]
                                       (if (= (:hash tx) highlighted-transaction)
                                         (assoc tx :highlighted? true)
                                         tx)))
        from-active-address-only? (filter #(= active-address (get-in % [:tx-opts :from])))))))

(reg-sub
  :district0x/tx-pending?
  :<- [:district0x/active-address]
  :<- [:district0x/transaction-log]
  (fn [[active-address {:keys [:transactions :ids-by-form]}] [_ contract-key contract-method form-id]]
    (let [transaction-hash (first (get-in ids-by-form
                                          (remove nil? [contract-key contract-method active-address form-id])))]
      (when transaction-hash
        (-> transaction-hash
          transactions
          :block-hash
          empty?)))))

(reg-sub
  :district0x-emails.set-email/tx-pending?
  (fn [[_ address]]
    (subscribe [:district0x/tx-pending? :district0x-emails :set-email {:district0x-emails/address address}]))
  identity)

(reg-sub
  :district0x-emails
  (fn [db]
    (:district0x-emails db)))

(reg-sub
  :district0x-emails/active-address-has-email?
  :<- [:district0x/active-address]
  :<- [:district0x-emails]
  (fn [[active-address district-emails]]
    (not (empty? (get district-emails active-address)))))





