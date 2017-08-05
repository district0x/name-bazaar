(ns district0x.ui.db
  (:require
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3?]]
    [district0x.ui.utils :as d0x-ui-utils]
    [re-frame.core :refer [dispatch]]))

(s/def ::load-node-addresses? boolean?)
(s/def ::web3 (complement nil?))
(s/def ::node-url string?)
(s/def ::server-url string?)
(s/def ::contracts-not-found? boolean?)
(s/def ::handler keyword?)
(s/def ::route-params (s/map-of keyword? (some-fn number? string?)))
(s/def ::active-page (s/keys :req-un [::handler] :opt-un [::route-params]))
(s/def ::window-width-size int?)
(s/def ::open? boolean?)
(s/def ::message string?)
(s/def ::on-request-close fn?)
(s/def ::auto-hide-duration int?)
(s/def ::modal boolean?)
(s/def ::title string?)
(s/def ::snackbar (s/keys :req-un [::open? ::message ::on-request-close ::auto-hide-duration]))
(s/def ::dialog (s/keys :req-un [::open? ::title ::modal]))
(s/def ::name string?)
(s/def ::address string?)
(s/def ::bin string?)
(s/def ::abi array?)
(s/def ::setter? boolean?)
(s/def ::smart-contracts (s/map-of keyword? (s/keys :req-un [::name] :opt-un [::setter? ::address ::bin ::abi])))
(s/def ::my-addresses (s/coll-of string?))
(s/def ::active-address (s/nilable address?))
(s/def ::conversion-rates (s/map-of number? number?))
(s/def ::load-conversion-rates-interval (s/nilable int?))
(s/def ::blockchain-connection-error? boolean?)
(s/def ::balances (s/map-of address? (s/map-of keyword? not-neg?)))
(s/def ::ui-disabled? boolean?)

(s/def ::contract-method keyword?)
(s/def ::contract-address address?)
(s/def :form-id/contract-address (s/keys :req-un [::contract-address]))
(s/def :form-id/nil nil?)
(s/def ::form-key keyword?)
(s/def ::form-field keyword?)
(s/def :district0x.db.ui.form/errors (s/coll-of keyword?))
(s/def :district0x.db.ui.form/data (s/map-of ::form-field any?))
(s/def ::form (s/keys :opt-un [:district0x.db.ui.form/errors :district0x.db.ui.form/data]))
(s/def ::form-id (s/nilable (s/map-of keyword? any?)))
(s/def ::form-id-keys (s/coll-of keyword?))

(s/def ::nil-id-form (s/map-of :form-id/nil ::form))
(s/def ::contract-address-id-form (s/map-of :form-id/contract-address ::form))

(s/def :transaction/block-hash sha3?)
(s/def :transaction/hash sha3?)
(s/def :transaction/value not-neg?)
(s/def :transaction/gas-used not-neg?)
(s/def :transaction/gas not-neg?)
(s/def :transaction/status (partial contains? #{:tx.status/pending :tx.status/not-loaded :tx.status/success
                                                :tx.status/failure}))

(s/def ::sender address?)
(s/def ::transaction-id sha3?)
(s/def ::transaction-id-list (s/coll-of ::transaction-id :kind list?))
(s/def ::transaction-name string?)
(s/def ::transactions (s/map-of ::transaction-id (s/keys :req-un [::form-key
                                                                  ::form-data
                                                                  ::form-id
                                                                  ::tx-opts
                                                                  :transaction/hash
                                                                  :transaction/status]
                                                         :opts-un [:transaction/block-hash
                                                                   :transaction/gas-used
                                                                   :transaction/gas
                                                                   :transaction/value])))
(s/def ::transaction-ids-chronological ::transaction-id-list)
(s/def ::transaction-ids-by-form (s/map-of ::form-key
                                           (s/map-of ::sender
                                                     (s/map-of ::form-id ::transaction-id-list))))

(s/def ::order-by-dir (partial contains? #{:asc :desc}))
(s/def ::order-by (s/map-of any? ::order-by-dir))
(s/def ::offset integer?)
(s/def ::limit integer?)
(s/def ::infinite-scroll (s/keys :opt-un [::offset ::limit]))
(s/def ::search-results (s/keys :opt-un [::ids ::loading? ::infinite-scroll]))

(s/def ::items (s/coll-of any?))

(s/def ::form-data (s/map-of keyword? any?))
(s/def ::default-data ::form-data)
(s/def ::tx-opts (s/map-of keyword? any?))


(s/def ::form-data-order (s/coll-of ::form-field))
(s/def ::wei-keys (s/coll-of ::form-field :kind set?))
(s/def ::contract-method-configs (s/map-of ::contract-method (s/keys :opts-un [::form-data-order
                                                                               ::wei-keys])))

(s/def ::form-configs (s/map-of ::form-key (s/keys :req-un [::contract-method
                                                            ::transaction-name
                                                            ::tx-opts]
                                                   :opt-un [::form-id-keys
                                                            ::default-data])))

(s/def :district0x.db.query-param/name string?)
(s/def :district0x.db.query-param/parser fn?)
(s/def ::form-field->query-param (s/map-of ::form-field (s/keys :req-un [:district0x.db.query-param/name]
                                                                :opt-un [:district0x.db.query-param/parser])))

(s/def ::route-handler->form-key (s/map-of ::handler ::form-key))
(s/def :form.district0x-emails/set-email (s/map-of :form-id/nil ::form))

(s/def ::db (s/keys :req-un [::active-address
                             ::blockchain-connection-error?
                             ::contracts-not-found?
                             ::dialog
                             ::my-addresses
                             ::node-url
                             ::server-url
                             ::smart-contracts
                             ::snackbar
                             ::web3
                             ::ui-disabled?
                             ::transactions
                             ::transaction-ids-chronological
                             ::transaction-ids-by-form
                             ::contract-method-configs
                             ::form-configs
                             ::form-field->query-param
                             ::route-handler->form-key]
                    :opt-un [::active-page
                             ::balances
                             ::conversion-rates
                             ::load-conversion-rates-interval
                             ::load-node-addresses?
                             :form.district0x-emails/set-email]))

(def default-db
  {:web3 nil
   :contracts-not-found? false
   :window-width-size (d0x-ui-utils/get-window-width-size js/window.innerWidth)
   :ui-disabled? false
   :snackbar {:open? false
              :message ""
              :auto-hide-duration 5000
              :on-request-close #(dispatch [:district0x.snackbar/close])}
   :dialog {:open? false
            :modal false
            :title ""
            :actions []
            :body ""
            :on-request-close #(dispatch [:district0x.dialog/close])}
   :smart-contracts {}
   :my-addresses []
   :active-address nil
   :blockchain-connection-error? false
   :conversion-rates {}
   :balances {}
   :transactions {}
   :transaction-ids-chronological '()
   :transaction-ids-by-form {}
   })




