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
     :routes constants/routes
     :contract-method-configs constants/contract-method-configs
     :form-configs constants/form-configs
     :form-field->query-param constants/form-field->query-param
     :route-handler->form-key constants/route-handler->form-key

     :offering-registry/offerings {}
     :offering-requests/requests {}
     :ens/records {}

     :form.ens/set-owner {}
     :form.buy-now-offering-factory/create-offering {}
     :form.buy-now-offering/buy {}
     :form.buy-now-offering/set-settings {}
     :form.auction-offering-factory/create-offering {}
     :form.auction-offering/bid {}
     :form.auction-offering/finalize {}
     :form.auction-offering/withdraw {}
     :form.auction-offering/set-settings {}
     :form.offering/reclaim-ownership {}
     :form.offering-requests/add-request {}
     :form.district0x-emails/set-email {}

     :search-results/offerings {}
     :search-results/offering-requests {}

     :search-form/search-offerings {:data {:offering/node-owner? true
                                           :order-by [[:offering/created-on :desc]]}}

     :search-form/search-offering-requests {:data {:order-by [[:offering-request/requesters-count :desc]]
                                                   :offering-request/name ""}}

     :search-form/home-page-search {:data {:offering/node-owner? true
                                           :offering/name ""
                                           :limit 4}}

     :search-form/watched-names {:data {:watched-names/ens-records []
                                        :watched-names/new-name ""}}

     }))