(ns name-bazaar.ui.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]))

(def contracts-version "1.0.0")
(def registrar-root ".eth")

(def default-gas-price 4000000000)

(def form-field->query-param
  {:offering/name {:name "name"}
   :offering/min-price {:name "min-price" :parser js/parseInt}
   :offering/max-price {:name "max-price" :parser js/parseInt}
   :offering/max-end-time {:name "max-end-time"}
   :offering/type {:name "type" :parser keyword}
   :offering-request/name {:name "name"}})

(def route-handler->form-key
  {:route.offerings/search :search-form/search-offerings
   :route.offering-requests/search :search-form/search-offering-requests})

(def routes
  ["/" [[["name/" :ens.record/name] :route.ens-record/detail]
        ["watched-names" :route/watched-names]
        [["user/" :offering/original-owner "/offerings"] :route.user/offerings]
        [["user/" :offering/original-owner "/purchases"] :route.user/purchases]
        [["user/" :offering/original-owner "/bids"] :route.user/bids]
        ["my-settings" :route.user/my-settings]
        ["my-offerings" :route.user/my-offerings]
        ["my-purchases" :route.user/my-purchases]
        ["my-bids" :route.user/my-bids]
        ["offering/create" :route.offering/create]
        [["offering/" :offering/address] :route.offering/detail]
        [["offering/" :offering/address "/edit"] :route.offering/edit]
        ["offerings" :route.offerings/search]
        ["offering-requests" :route.offering-requests/search]
        ["about" :route/about]
        ["how-it-works" :route/how-it-works]
        [true :route/home]]])

(def transaction-log-tx-name-templates
  {:ens/set-owner ["Transfer %s ownership" :ens.record/name]
   :registrar/transfer ["Transfer %s ownership" (comp #(str % ".eth") :ens.record/label)]
   :mock-registrar/register ["Register %s" (comp #(str % ".eth") :ens.record/label)]
   :auction-offering/set-settings ["Edit %s auction" :offering/name]
   :buy-now-offering-factory/create-offering ["Create %s offering" :offering/name]
   :auction-offering-factory/finalize ["Finalize %s auction for" :offering/name]
   :buy-now-offering/buy ["Buy %s" :offering/name]
   :auction-offering/withdraw ["Withdraw from %s auction" :offering/name]
   :auction-offering-factory/create-offering ["Create %s auction" :offering/name]
   :auction-offering/bid ["Bid for %s" :offering/name]
   :buy-now-offering/set-settings ["Edit %s offering" :offering/name]
   :buy-now-offering/reclaim-ownership ["Reclaim ownership from %s offering" :offering/name]
   :offering-requests/add-request ["Request %s for offering" :ens.record/name]})

(def transaction-log-on-item-click-routes
  {:ens/set-owner [:route.ens-record/detail #(select-keys % [:ens.record/name])]
   :registrar/transfer [:route.ens-record/detail #(hash-map :ens.record/name (str (:ens.record/label %) registrar-root))]
   :mock-registrar/register [:route.ens-record/detail #(hash-map :ens.record/name (str (:ens.record/label %) registrar-root))]
   :auction-offering/set-settings [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering-factory/create-offering [:route.ens-record/detail #(hash-map :ens.record/name (:offering/name %))]
   :auction-offering-factory/finalize [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering/buy [:route.offering/detail #(select-keys % [:offering/address])]
   :auction-offering/withdraw [:route.offering/detail #(select-keys % [:offering/address])]
   :auction-offering-factory/create-offering [:route.ens-record/detail #(hash-map :ens.record/name (:offering/name %))]
   :auction-offering/bid [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering/set-settings [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering/reclaim-ownership [:route.offering/detail #(select-keys % [:offering/address])]
   :offering-requests/add-request [:route.ens-record/detail #(select-keys % [:ens.record/name])]})
