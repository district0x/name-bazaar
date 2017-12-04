(ns name-bazaar.ui.events.reverse-registrar-events)

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
