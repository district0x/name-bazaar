(ns name-bazaar.ui.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]))

(def contracts-version "1.0.0")

(def contract-method-configs
  {:ens/set-owner
   {:args-order [:ens.record/node :ens.record/owner]}

   :registrar/transfer
   {:args-order [:ens.record/label-hash :ens.record/owner]}

   :auction-offering/set-settings
   {:args-order [:offering/address
                 :offering/price
                 :auction-offering/end-time
                 :auction-offering/extension-duration
                 :auction-offering/min-bid-increase]
    :wei-keys #{:offering/price}
    :contract-address-key :offering/address}

   :buy-now-offering-factory/create-offering
   {:args-order [:offering/name
                 :offering/price]
    :wei-keys #{:offering/price}}

   :auction-offering-factory/finalize
   {:args-order [:offering/address
                 :auction-offering/transfer-price?]
    :contract-address-key :offering/address}

   :buy-now-offering/buy
   {:contract-address-key :offering/address
    :ether-value-key :offering/price
    :wei-keys #{:offering/price}}

   :auction-offering-factory/withdraw
   {:args-order [:offering/address
                 :auction-offering/bidder]
    :contract-address-key :offering/address}

   :auction-offering-factory/create-offering
   {:args-order [:offering/name
                 :auction-offering/end-time
                 :auction-offering/extension-duration
                 :auction-offering/min-bid-increase]
    :wei-keys #{:offering/price :auction-offering/min-bid-increase}}

   :auction-offering/bid
   {:ether-value-key :offering/price
    :contract-address-key :offering/address
    :wei-keys #{:offering/price}}

   :buy-now-offering/set-settings
   {:args-order [:offering/price]
    :contract-address-key :offering/address
    :wei-keys #{:offering/price}}

   :buy-now-offering/reclaim-ownership
   {:contract-address-key :offering/address}

   :offering-requests/add-request
   {:args-order [:ens.record/name]}

   :mock-registrar/register
   {:args-order [:ens.record/label-hash]}})

(def gas-price 4000000000)

(def form-configs
  {:form.registrar/transfer
   {:contract-method :registrar/transfer
    :form-id-keys [:ens.record/label-hash]
    :tx-opts {:gas 300000 :gas-price gas-price}}

   :form.auction-offering/withdraw
   {:contract-method :auction-offering/withdraw
    :form-id-keys [:contract/address]
    :tx-opts {:gas 70000 :gas-price gas-price}
    :default-data {:auction-offering/bidder nil}}

   :form.auction-offering-factory/create-offering
   {:contract-method :auction-offering-factory/create-offering
    :tx-opts {:gas 700000 :gas-price gas-price}
    :default-data {:offering/name ""
                   :offering/price 0.01
                   :auction-offering/end-time (to-epoch (t/plus (t/now) (t/weeks 1)))
                   :auction-offering/extension-duration (t/in-seconds (t/hours 1))
                   :auction-offering/min-bid-increase 0.01}}

   :form.offering-requests/add-request
   {:contract-method :offering-requests/add-request
    :form-id-keys [:offering-request/name]
    :tx-opts {:gas 100000 :gas-price gas-price}}

   :form.auction-offering/bid
   {:contract-method :auction-offering/bid
    :form-id-keys [:offering/address]
    :tx-opts {:gas 70000 :gas-price gas-price}
    :default-data {:bid/value 0}}

   :form.offering/reclaim-ownership
   {:contract-method :buy-now-offering/reclaim-ownership
    :form-id-keys [:offering/address]
    :tx-opts {:gas 200000 :gas-price gas-price}}

   :form.buy-now-offering/set-settings
   {:contract-method :buy-now-offering/set-settings

    :form-id-keys [:offering/address]
    :tx-opts {:gas 250000 :gas-price gas-price}
    :default-data {:offering/price 0}}

   :form.auction-offering/finalize
   {:contract-method :auction-offering/finalize
    :form-id-keys [:offering/address]
    :tx-opts {:gas 70000 :gas-price gas-price}
    :default-data {:auction-offering/transfer-price? true}}

   :form.buy-now-offering/buy
   {:contract-method :buy-now-offering/buy
    :form-id-keys [:offering/address]
    :tx-opts {:gas 100000 :gas-price gas-price}}

   :form.auction-offering/set-settings
   {:contract-method :auction-offering/set-settings
    :form-id-keys [:offering/address]
    :tx-opts {:gas 1000000 :gas-price gas-price}}

   :form.buy-now-offering-factory/create-offering
   {:contract-method :buy-now-offering-factory/create-offering
    :tx-opts {:gas 700000 :gas-price gas-price}
    :default-data {:offering/name ""
                   :offering/price 0.01}}
   :form.ens/set-owner
   {:contract-method :ens/set-owner
    :form-id-keys [:ens.record/node]
    :tx-opts {:gas 100000 :gas-price gas-price}}

   :form.mock-registrar/register
   {:contract-method :mock-registrar/register
    :tx-opts {:gas 700000 :gas-price gas-price}}})

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
   :auction-offering-factory/withdraw ["Withdraw from %s auction" :offering/name]
   :auction-offering-factory/create-offering ["Create %s auction" :offering/name]
   :auction-offering/bid ["Bid for %s" :offering/name]
   :buy-now-offering/set-settings ["Edit %s offering" :offering/name]
   :buy-now-offering/reclaim-ownership ["Reclaim ownership from %s offering" :offering/name]
   :offering-requests/add-request ["Request %s for offering" :ens.record/name]})

(def transaction-log-on-item-click-routes
  {:ens/set-owner [:route.ens-record/detail #(select-keys % [:ens.record/name])]
   :registrar/transfer [:route.ens-record/detail #(hash-map :ens.record/name (str (:ens.record/label %) ".eth"))]
   :mock-registrar/register [:route.ens-record/detail #(hash-map :ens.record/name (str (:ens.record/label %) ".eth"))]
   :auction-offering/set-settings [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering-factory/create-offering [:route.ens-record/detail #(hash-map :ens.record/name (:offering/name %))]
   :auction-offering-factory/finalize [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering/buy [:route.offering/detail #(select-keys % [:offering/address])]
   :auction-offering-factory/withdraw [:route.offering/detail #(select-keys % [:offering/address])]
   :auction-offering-factory/create-offering [:route.ens-record/detail #(hash-map :ens.record/name (:offering/name %))]
   :auction-offering/bid [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering/set-settings [:route.offering/detail #(select-keys % [:offering/address])]
   :buy-now-offering/reclaim-ownership [:route.offering/detail #(select-keys % [:offering/address])]
   :offering-requests/add-request [:route.ens-record/detail #(select-keys % [:ens.record/name])]})
