(ns name-bazaar.ui.db
  (:require
   [cljs-time.coerce :refer [to-epoch]]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3]
   [cljs.spec.alpha :as s]
   [district0x.shared.utils :as d0x-shared-utils :refer [sha3? address? date? not-neg?]]
   [district0x.ui.db]
   [district0x.ui.history :as history]
   [district0x.ui.utils :refer [get-window-size namehash match-current-location]]
   [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
   [name-bazaar.ui.config :refer [config]]
   [name-bazaar.ui.constants :as constants]
   [re-frame.core :refer [dispatch]]
   ))

(def default-db
  (merge
    district0x.ui.db/default-db
    config
    {:active-page (if history/hashroutes?
                    (match-current-location constants/routes)
                    (match-current-location constants/routes (history/get-state)))
     :smart-contracts smart-contracts
     :now (t/now)

     :offerings-main-search-drawer {:open? false}

     :offerings {}
     :offering-requests {}
     :ens/records {}
     :name-bazaar-registrar/entries {}
     :registration-bids {}
     :public-resolver/reverse-records {}
     :public-resolver/records {}
     :district0x-emails {}

     :offerings/total-count nil

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
                                         :order-by :offering/created-on
                                         :order-by-dir :desc
                                         :total-count? true
                                         :offset 0
                                         :limit constants/infinite-lists-init-load-limit}}
                  :ens-record-offerings {:ids []
                                         :params {:order-by :offering/created-on
                                                  :order-by-dir :desc
                                                  :total-count? true
                                                  :offset 0
                                                  :limit constants/infinite-lists-init-load-limit}}
                  :user-offerings {:ids []
                                   :params {:open? true
                                            :finalized? true
                                            :order-by :offering/created-on
                                            :order-by-dir :desc
                                            :total-count? true
                                            :offset 0
                                            :limit constants/infinite-lists-init-load-limit}}
                  :similar-offerings {:ids []
                                      :params {:order-by :name-relevance
                                               :order-by-dir :desc
                                               :name-position :end
                                               :node-owner? true
                                               :min-end-time-now? true
                                               :total-count? true
                                               :offset 0
                                               :limit constants/infinite-lists-init-load-limit}}
                  :user-bids {:ids []
                              :params {:order-by :auction-offering/end-time
                                       :order-by-dir :asc
                                       :min-end-time-now? false
                                       :winning? true
                                       :outbid? true
                                       :auction? true
                                       :total-count? true
                                       :offset 0
                                       :limit constants/infinite-lists-init-load-limit}}
                  :user-purchases {:ids []
                                   :params {:order-by :offering/finalized-on
                                            :order-by-dir :desc
                                            :total-count? true
                                            :offset 0
                                            :limit constants/infinite-lists-init-load-limit}}}
      :offering-requests {:main-search {:ids []
                                        :params {:name-position :any
                                                 :order-by :offering-request/requesters-count
                                                 :order-by-dir :desc
                                                 :total-count? true
                                                 :offset 0
                                                 :limit constants/infinite-lists-init-load-limit}}}}

     :saved-searches {:offerings-search {}}
     :watched-names {:ens/records {} :order '()}
     :infinite-list {:expanded-items {}}}))
