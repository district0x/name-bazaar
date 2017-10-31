(ns district0x.ui.db
  (:require
   [cljs-web3.core :as web3]
   [cljs.spec.alpha :as s]
   [district0x.shared.utils :as d0x-shared-utils :refer [address? not-neg? sha3?]]
   [district0x.ui.utils :refer [get-window-size namehash]]
   [re-frame.core :refer [dispatch]]))

(def default-db
  {:web3 nil
   :contracts-not-found? false
   :window {:focused? true
            :size (get-window-size js/window.innerWidth)}
   :ui-disabled? false
   :config {}
   :snackbar {:open? false
              :message ""
              :action-href nil
              :timeout 6000}
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

