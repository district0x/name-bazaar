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
     :now (t/now)

     :offerings-main-search-drawer {:open? false}

     :offerings {}
     :offering-requests {}
     :ens/records {}
     :registrar/entries {}

     :search-results
     {:offerings {:home-page-autocomplete {:ids []
                                           :params {:limit 5
                                                    :node-owner? true}}
                  :main-search {:ids []
                                :params {:name-position :any
                                         :buy-now? true
                                         :auction? true
                                         :top-level-names? true
                                         :exclude-special-chars? true
                                         :node-owner? true
                                         :min-end-time-now? true
                                         :order-by-columns [:created-on]
                                         :order-by-dirs [:desc]
                                         :offset 0
                                         :limit constants/infinite-lists-init-load-limit}}
                  :ens-record-offerings {:ids []
                                         :params {:order-by-columns [:created-on]
                                                  :order-by-dirs [:desc]
                                                  :offset 0
                                                  :limit constants/infinite-lists-init-load-limit}}
                  :user-offerings {:ids []
                                   :params {}}
                  :similar-offerings {:ids []
                                      :params {:order-by-columns [:name-relevance]
                                               :order-by-dirs [:desc]
                                               :name-position :end
                                               :node-owner? true
                                               :min-end-time-now? true
                                               :offset 0
                                               :limit constants/infinite-lists-init-load-limit}}
                  :user-bids {:ids []
                              :params {:order-by-columns [:end-time]
                                       :order-by-dirs [:asc]
                                       :min-end-time-now? true
                                       :auction? true
                                       :offset 0
                                       :limit constants/infinite-lists-init-load-limit}}
                  :user-purchases {:ids []
                                   :params {:order-by-columns [:finalized-on]
                                            :order-by-dirs [:desc]
                                            :offset 0
                                            :limit constants/infinite-lists-init-load-limit}}}
      :offering-requests {:main-search {:ids []
                                        :params {:name-position :any
                                                 :order-by-columns [:requesters-count]
                                                 :order-by-dirs [:desc]
                                                 :offset 0
                                                 :limit constants/infinite-lists-init-load-limit}}}}

     :saved-searches {:offerings-search {}}
     :watched-names {:ens/records {} :order []}
     :infinite-list {:expanded-items {}}}))