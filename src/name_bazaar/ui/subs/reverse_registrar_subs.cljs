(ns name-bazaar.ui.subs.reverse-registrar-subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :reverse-registrar.claim-with-resolver/tx-pending?
  (fn [[_ addr]]
    [(subscribe [:district0x/tx-pending? :reverse-registrar :claim-with-resolver {:ens.record/addr addr}])])
  first)
