(ns district0x.ui.db
  (:require
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3?]]
    [district0x.ui.utils :as d0x-ui-utils]
    [re-frame.core :refer [dispatch]]))

(def default-db
  {:web3 nil
   :contracts-not-found? false
   :window-width-size (d0x-ui-utils/get-window-width-size js/window.innerWidth)
   :ui-disabled? false
   :snackbar {:open? false
              :message ""
              :auto-hide-duration 6000
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
   :transaction-log {:transactions {}
                     :ids-chronological '()
                     :ids-by-form {}
                     :settings {:from-active-address-only? false}}})




