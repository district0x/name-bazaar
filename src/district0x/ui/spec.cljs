(ns district0x.ui.spec
  (:require
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3? date?]]))

(s/def :db/load-node-addresses? boolean?)
(s/def :db/web3 (complement nil?))
(s/def :db/node-url string?)
(s/def :db/server-url string?)
(s/def :db/contracts-not-found? boolean?)
(s/def :route/handler keyword?)
(s/def :route/route-params (s/map-of keyword? (some-fn number? string?)))
(s/def :db/active-page (s/keys :req-un [:route/handler] :opt-un [:route/route-params]))
(s/def :db/window-width-size int?)
(s/def :snackbar/open? boolean?)
(s/def :dialog/open? boolean?)
(s/def :drawer/open? boolean?)
(s/def :snackbar/message string?)
(s/def :snackbar/on-request-close fn?)
(s/def :snackbar/auto-hide-duration int?)
(s/def :dialog/modal boolean?)
(s/def :dialog/title string?)
(s/def :db/snackbar (s/keys :req-un [:snackbar/open? :snackbar/message :snackbar/on-request-close :snackbar/auto-hide-duration]))
(s/def :db/dialog (s/keys :req-un [:dialog/open? :dialog/title :dialog/modal]))
(s/def :db/menu-drawer (s/keys :req-un [:drawer/open?]))

(s/def :contract/key keyword?)
(s/def :contract/name string?)
(s/def :contract/method keyword?)
(s/def :contract/address address?)
(s/def :contract/bin string?)
(s/def :contract/abi array?)
(s/def :db/smart-contracts (s/map-of :contract/key (s/keys :req-un [:contract/name] :opt-un [:contract/address :contract/bin :contract/abi])))
(s/def :db/my-addresses (s/coll-of string?))
(s/def :db/active-address (s/nilable address?))
(s/def :conversion-rate/currency keyword?)
(s/def :db/conversion-rates (s/map-of :conversion-rate/currency number?))
(s/def :db/load-conversion-rates-interval (s/nilable int?))
(s/def :db/blockchain-connection-error? boolean?)
(s/def :balance/currency keyword?)
(s/def :db/balances (s/map-of address? (s/map-of :balance/currency not-neg?)))
(s/def :db/ui-disabled? boolean?)

(s/def :form/key keyword?)
(s/def :form/field keyword?)
(s/def :form/errors (s/coll-of keyword?))
(s/def :form/data (s/map-of :form/field any?))
(s/def :db/form (s/keys :opt-un [:form/errors :form/data]))
(s/def :form/id (s/nilable :form/data))
(s/def :form/id-keys (s/coll-of :form/field))

(s/def :transaction/block-hash sha3?)
(s/def :transaction/hash sha3?)
(s/def :transaction/value not-neg?)
(s/def :transaction/gas-used not-neg?)
(s/def :transaction/gas not-neg?)
(s/def :transaction/status (partial contains? #{:tx.status/pending :tx.status/not-loaded :tx.status/success
                                                :tx.status/failure}))

(s/def :transaction/sender address?)
(s/def :transaction/form-data :form/data)
(s/def :transaction/form-key :form/key)
(s/def :transaction/form-id :form/id)
(s/def :transaction/tx-opts (s/map-of keyword? any?))
(s/def :transaction/created-on not-neg?)
(s/def :db/transactions (s/map-of :transaction/hash (s/keys :req-un [:transaction/form-key
                                                                     :transaction/form-data
                                                                     :transaction/tx-opts
                                                                     :transaction/hash
                                                                     :transaction/status]
                                                            :opts-un [:transaction/form-id
                                                                      :transaction/block-hash
                                                                      :transaction/gas-used
                                                                      :transaction/gas
                                                                      :transaction/value
                                                                      :transaction/created-on])))
(s/def :db/transaction-ids-chronological (s/coll-of :transaction/hash :kind list?))
(s/def :db/transaction-ids-by-form (s/map-of :form/key
                                             (s/map-of :transaction/sender
                                                       (s/or
                                                         :with-form-id (s/map-of :form/id :db/transaction-ids-chronological)
                                                         :without-form-id :db/transaction-ids-chronological))))

(s/def :transaction-log-settings/from-active-address-only? (s/nilable boolean?))
(s/def :db/transaction-log-settings (s/keys :req-un [:transaction-log-settings/from-active-address-only?]))

(s/def :search-params/order-by-dir (partial contains? #{:asc :desc}))
(s/def :search-params/order-by (s/coll-of (s/tuple keyword? :search-params/order-by-dir)))
(s/def :search-params/offset integer?)
(s/def :search-params/limit integer?)
(s/def :search-results/ids (s/coll-of any?))
(s/def :search-results/total-count (s/nilable not-neg?))
(s/def :search-results/loading? boolean?)

(s/def :db/search-results (s/keys :opt-un [:search-results/ids
                                           :search-results/loading?
                                           :search-results/total-count]))

(s/def :contract-method-config/args-order (s/coll-of :form/field))
(s/def :contract-method-config/wei-keys (s/coll-of :form/field :kind set?))
(s/def :contract-method-config/contract-address-key keyword?)
(s/def :contract-method-config/ether-value-key keyword?)

(s/def :db/contract-method-configs (s/map-of :contract/method
                                             (s/keys :opts-un [:contract-method-config/args-order
                                                               :contract-method-config/wei-keys
                                                               :contract-method-config/contract-address-key
                                                               :contract-method-config/ether-value-key])))


(s/def :form-config/default-data :form/data)
(s/def :form-config/contract-method :contract/method)
(s/def :form-config/form-id-keys :form/id-keys)

(s/def :db/form-configs (s/map-of :form/key (s/keys :req-un [:form-config/contract-method
                                                             :transaction/tx-opts]
                                                    :opt-un [:form-config/form-id-keys
                                                             :form-config/default-data])))

(s/def :route.query-param/name string?)
(s/def :route.query-param/parser fn?)
(s/def :db/form-field->query-param (s/map-of :form/field (s/keys :req-un [:route.query-param/name]
                                                                 :opt-un [:route.query-param/parser])))

(s/def :db/route-handler->form-key (s/map-of :route/handler :form/key))

(s/def :district0x-emails/email string?)
(s/def :form.district0x-emails/set-email (s/merge :db/form (s/keys :opt [:district0x-emails/email])))

(s/def :db/routes (some-fn vector? map?))

(s/def :district0x.ui/db (s/keys :req-un [:db/active-address
                                          :db/blockchain-connection-error?
                                          :db/contracts-not-found?
                                          :db/dialog
                                          :db/my-addresses
                                          :db/node-url
                                          :db/server-url
                                          :db/smart-contracts
                                          :db/snackbar
                                          :db/web3
                                          :db/ui-disabled?
                                          :db/transactions
                                          :db/transaction-ids-chronological
                                          :db/transaction-ids-by-form
                                          :db/transaction-log-settings
                                          :db/contract-method-configs
                                          :db/form-configs
                                          :db/form-field->query-param
                                          :db/route-handler->form-key
                                          :db/routes]
                                 :opt-un [:db/active-page
                                          :db/menu-drawer
                                          :db/balances
                                          :db/conversion-rates
                                          :db/load-conversion-rates-interval
                                          :db/load-node-addresses?
                                          :form.district0x-emails/set-email]))
