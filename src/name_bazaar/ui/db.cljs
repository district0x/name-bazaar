(ns name-bazaar.ui.db
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [sha3? address? date? not-neg?]]
    [district0x.ui.db]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [dispatch]]
    [district0x.ui.utils :as d0x-ui-utils]))

(def default-db
  (merge
    district0x.ui.db/default-db
    {:node-url #_"https://mainnet.infura.io/" "http://localhost:8549"
     :server-url "http://localhost:6200"
     :active-page (d0x-ui-utils/match-current-location constants/routes)
     :smart-contracts smart-contracts

     :offerings-search-params-drawer {:open? false}

     :offering-registry/offerings {}
     :offering-requests/requests {}
     :ens/records {}
     :registrar/entries {}

     :search-results/home-page-autocomplete {:loading? false
                                             :ids []
                                             :params {:limit 5
                                                      :node-owner? true}}

     :search-results/offerings-main-search {:loading? false
                                            :ids []
                                            :params {:name-position :contain
                                                     :buy-now? true
                                                     :auction? true
                                                     :top-level-names? true
                                                     :exclude-special-chars? true
                                                     :node-owner? true
                                                     :order-by-columns [:created-on]
                                                     :order-by-dirs [:desc]
                                                     :offset 0
                                                     :limit constants/infinite-lists-init-load-limit}}

     :search-results/offering-requests-main-search {:loading? false :ids []}

     :search-results/name-offerings {:loading? false :ids []}
     :search-results/user-offerings {:loading? false :ids []}
     :search-results/similiar-offerings {:loading? false :ids []}
     :search-results/bid-offerings {:loading? false :ids []}
     :search-results/purchased-offerings {:loading? false :ids []}

     :saved-searches {:offerings-search {}}

     :infinite-list {:expanded-items {}}}))