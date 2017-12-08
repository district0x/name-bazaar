(ns name-bazaar.ui.events.reverse-registrar-events
  (:require
   [cljs.spec.alpha :as s]
   [district0x.ui.spec-interceptors :refer [validate-first-arg]]
   [district0x.ui.utils :as d0x-ui-utils :refer [path-with-query]]
   [goog.string :as gstring]
   [goog.string.format]
   [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
   [name-bazaar.ui.utils :refer [reverse-record-node namehash sha3 parse-query-params path-for get-ens-record get-offering-name get-offering]]
   [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
   [district0x.shared.utils :as d0x-shared-utils]
   [medley.core :as medley]
   [taoensso.timbre :as logging :refer-macros [info warn error]]))

(reg-event-fx
 :reverse-registrar/claim-with-resolver
 [interceptors (validate-first-arg (s/keys :req [:ens.record/address]))]
 (fn [{:keys [:db]} [form-data]]
   {:dispatch [:district0x/make-transaction
               {:name (gstring/format "Setting reverse resolver for %s" (:ens.record/address form-data))
                :contract-key :reverse-registrar
                :contract-method :claim-with-resolver
                :form-data (select-keys form-data [:ens.record/address :public-resolver])
                :args-order [:ens.record/address :public-resolver]
                :form-id (select-keys form-data [:ens.record/address])
                :tx-opts {:gas 100000 :gas-price default-gas-price}
                :on-tx-receipt [:district0x.snackbar/show-message
                                (gstring/format "Resolver for %s is set to standard." (:ens.record/address form-data))]}]}))
