(ns name-bazaar.ui.db
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.spec.alpha :as s]
    [district0x.shared.utils :as d0x-shared-utils :refer [sha3? address? date? not-neg?]]
    [district0x.ui.db]
    [district0x.ui.history :as history]
    [district0x.ui.utils :as d0x-ui-utils]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [dispatch]]))

(def development-config
  {:node-url "http://localhost:8549"
   :load-node-addresses? true
   :server-url "http://localhost:6200"})

(def production-config
  {:node-url "https://ropsten.infura.io/"
   :load-node-addresses? false
   :server-url "https://api.namebazaar.io"})

(def default-db
  (merge
    district0x.ui.db/default-db
    development-config
    ;production-config
    {:active-page (if (d0x-ui-utils/hashroutes?)
                    (d0x-ui-utils/match-current-location constants/routes)
                    (d0x-ui-utils/match-current-location constants/routes (history/get-state)))
     :smart-contracts smart-contracts
     :now (t/now)

     :offerings-main-search-drawer {:open? false}

     :offerings {}
     :offering-requests {}
     :ens/records {}
     :registrar/entries {}
     :district0x-emails {}

     :search-results
     {:offerings {:home-page-autocomplete {:ids []}
                  :home-page-newest {:ids [] :loading? true}
                  :home-page-most-active {:ids [] :loading? true}
                  :home-page-ending-soon {:ids [] :loading? true}
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
                                         :total-count? true
                                         :offset 0
                                         :limit constants/infinite-lists-init-load-limit}}
                  :ens-record-offerings {:ids []
                                         :params {:order-by-columns [:created-on]
                                                  :order-by-dirs [:desc]
                                                  :total-count? true
                                                  :offset 0
                                                  :limit constants/infinite-lists-init-load-limit}}
                  :user-offerings {:ids []
                                   :params {:open? true
                                            :finalized? true
                                            :order-by-columns [:created-on]
                                            :order-by-dirs [:desc]
                                            :total-count? true
                                            :offset 0
                                            :limit constants/infinite-lists-init-load-limit}}
                  :similar-offerings {:ids []
                                      :params {:order-by-columns [:name-relevance]
                                               :order-by-dirs [:desc]
                                               :name-position :end
                                               :node-owner? true
                                               :min-end-time-now? true
                                               :total-count? true
                                               :offset 0
                                               :limit constants/infinite-lists-init-load-limit}}
                  :user-bids {:ids []
                              :params {:order-by-columns [:end-time]
                                       :order-by-dirs [:asc]
                                       :min-end-time-now? false
                                       :winning? true
                                       :outbid? true
                                       :auction? true
                                       :total-count? true
                                       :offset 0
                                       :limit constants/infinite-lists-init-load-limit}}
                  :user-purchases {:ids []
                                   :params {:order-by-columns [:finalized-on]
                                            :order-by-dirs [:desc]
                                            :total-count? true
                                            :offset 0
                                            :limit constants/infinite-lists-init-load-limit}}}
      :offering-requests {:main-search {:ids []
                                        :params {:name-position :any
                                                 :order-by-columns [:requesters-count]
                                                 :order-by-dirs [:desc]
                                                 :total-count? true
                                                 :offset 0
                                                 :limit constants/infinite-lists-init-load-limit}}}}

     :saved-searches {:offerings-search {}}
     :watched-names {:ens/records {} :order '()}
     :infinite-list {:expanded-items {}}}))
