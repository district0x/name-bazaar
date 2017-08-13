(ns name-bazaar.ui.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]))

(def contracts-version "1.0.0")

(def contract-method-configs
  {:ens/set-owner
   {:form-data-order [:ens.record/node
                      :ens.record/owner]}

   :auction-offering/set-settings
   {:form-data-order [:contract-address
                      :offering/price
                      :auction-offering-factory/end-time
                      :auction-offering-factory/extension-duration
                      :auction-offering-factory/min-bid-increase]
    :wei-keys #{:offering/price}}

   :buy-now-offering-factory/create-offering
   {:form-data-order [:offering/name
                      :offering/price]
    :wei-keys #{:offering/price}}

   :auction-offering-factory/finalize
   {:form-data-order [:contract-address
                      :auction-offering/transfer-price?]}

   :buy-now-offering/buy
   {:form-data-order [:contract-address]}

   :auction-offering-factory/withdraw
   {:form-data-order [:contract-address
                      :auction-offering/bidder]}

   :auction-offering-factory/create-offering
   {:form-data-order [:offering/name
                      :auction-offering-factory/end-time
                      :auction-offering-factory/extension-duration
                      :auction-offering-factory/min-bid-increase]
    :wei-keys #{:offering/price :auction-offering-factory/min-bid-increase}}

   :auction-offering/bid
   {:form-data-order [:contract-address]}

   :buy-now-offering/set-settings
   {:form-data-order [:contract-address
                      :offering/price]
    :wei-keys #{:offering/price}}})

(def gas-price 4000000000)

(def form-configs
  {:form.auction-offering/withdraw
   {:contract-method :auction-offering/withdraw
    :transaction-name "Auction Bid Withdrawal"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 70000 :gas-price gas-price}
    :default-data {:auction-offering/bidder nil}}

   :form.auction-offering-factory/create-offering
   {:contract-method :auction-offering-factory/create-offering
    :transaction-name "New Auction Offering"
    :tx-opts {:gas 700000 :gas-price gas-price}
    :default-data {:offering/name ""
                   :offering/price 0.01
                   :auction-offering-factory/end-time (to-epoch (t/plus (t/now) (t/weeks 1)))
                   :auction-offering-factory/extension-duration (t/in-seconds (t/hours 1))
                   :auction-offering-factory/min-bid-increase 0.01}}

   :form.offering-requests/add-request
   {:contract-method :offering-requests/add-request
    :transaction-name "New Offering Request"
    :form-id-keys [:offering-request/name]
    :tx-opts {:gas 100000 :gas-price gas-price}}

   :form.auction-offering/bid
   {:contract-method :auction-offering/bid
    :transaction-name "Auction Bid"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 70000 :gas-price gas-price}}

   :form.offering/reclaim-ownership
   {:contract-method :buy-now-offering/reclaim-ownership
    :transaction-name "Reclaim Offering Ownership"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 200000 :gas-price gas-price}}

   :form.buy-now-offering/set-settings
   {:contract-method :buy-now-offering/set-settings
    :transaction-name "Update Offering Settings"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 250000 :gas-price gas-price}
    :default-data {:offering/price 0}}

   :form.auction-offering/finalize
   {:contract-method :auction-offering/finalize
    :transaction-name "Finalize Auction"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 70000 :gas-price gas-price}
    :default-data {:auction-offering/transfer-price? true}}

   :form.buy-now-offering/buy
   {:contract-method :buy-now-offering/buy
    :transaction-name "Buy Now Offering"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 100000 :gas-price gas-price}}

   :form.auction-offering/set-settings
   {:contract-method :auction-offering/set-settings
    :transaction-name "Update Offering Settings"
    :form-id-keys [:contract-address]
    :tx-opts {:gas 1000000 :gas-price gas-price}}

   :form.buy-now-offering-factory/create-offering
   {:contract-method :buy-now-offering-factory/create-offering
    :transaction-name "New Instant Buy Offering"
    :tx-opts {:gas 500000 :gas-price gas-price}
    :default-data {:offering/name ""
                   :offering/price 0.01}}
   :form.ens/set-owner
   {:contract-method :ens/set-owner
    :transaction-name "ENS Ownership Transfer"
    :form-id-keys [:ens.record/node]
    :tx-opts {:gas 100000 :gas-price gas-price}}})

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
        ["my-settings" :route.user/my-settings]
        ["my-offerings" :route.user/my-offerings]
        ["offering/create" :route.offering/create]
        [["offering/" :offering/address] :route.offering/detail]
        ["offerings" :route.offerings/search]
        ["offering-requests" :route.offering-requests/search]
        ["about" :route/about]
        ["how-it-works" :route/how-it-works]
        [true :route/home]]])
