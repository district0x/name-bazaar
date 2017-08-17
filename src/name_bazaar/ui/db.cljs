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

     :offering-registry/offerings {}
     :offering-requests/requests {}
     :ens/records {}

     :search-results/offerings {}
     :search-results/offering-requests {}}))