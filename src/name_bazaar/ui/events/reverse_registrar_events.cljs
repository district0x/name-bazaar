(ns name-bazaar.ui.events.reverse-registrar-events
  (:require
   [cljs.spec.alpha :as s]
   [district0x.ui.spec-interceptors :refer [validate-first-arg]]
   [goog.string :as gstring]
   [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
   [re-frame.core :as re-frame :refer [reg-event-fx]]
   ))

(reg-event-fx
 :reverse-registrar/claim-with-resolver
 [interceptors (validate-first-arg (s/keys :req [:ens.record/addr]))]
 (fn [{:keys [:db]} [form-data]]
   {:dispatch [:district0x/make-transaction
               {:name (gstring/format "Setting reverse resolver for %s" (:ens.record/addr form-data))
                :contract-key :reverse-registrar
                :contract-method :claim-with-resolver
                :form-data (select-keys form-data [:ens.record/addr :public-resolver])
                :args-order [:ens.record/addr :public-resolver]
                :form-id (select-keys form-data [:ens.record/addr])
                :tx-opts {:gas 100000 :gas-price default-gas-price}
                :on-tx-receipt [:district0x.snackbar/show-message
                                (gstring/format "Resolver for %s is set to standard." (:ens.record/addr form-data))]}]}))
