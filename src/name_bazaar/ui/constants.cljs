(ns name-bazaar.ui.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [collify]]
    [district0x.ui.utils :refer [parse-boolean-string parse-kw-coll-query-params parse-int-or-nil parse-float-or-nil]]
    [medley.core :as medley]))

(def contracts-version "1.0.0")
(def registrar-root ".eth")

(def default-gas-price 4000000000)

(def infinite-lists-init-load-limit 25)
(def infinite-lists-next-load-limit 10)

(def query-params-parsers
  {:route.offerings/search {:name-position keyword
                            :min-length parse-int-or-nil
                            :max-length parse-int-or-nil
                            :buy-now? parse-boolean-string
                            :auction? parse-boolean-string
                            :exclude-numbers? parse-boolean-string
                            :exclude-special-chars? parse-boolean-string
                            :top-level-names? parse-boolean-string
                            :sub-level-names? parse-boolean-string
                            :order-by-dirs parse-kw-coll-query-params
                            :order-by-columns parse-kw-coll-query-params}
   :route.offering-requests/search {:name-position keyword}})

(def routes
  ["/" [[["name/" :ens.record/name] :route.ens-record/detail]
        ["register" :route.mock-registrar/register]
        ["watched-names" :route/watched-names]
        [["user/" :user/address "/offerings"] :route.user/offerings]
        [["user/" :user/address "/purchases"] :route.user/purchases]
        [["user/" :user/address "/bids"] :route.user/bids]
        ["my-settings" :route.user/my-settings]
        ["my-offerings" :route.user/my-offerings]
        ["my-purchases" :route.user/my-purchases]
        ["my-bids" :route.user/my-bids]
        ["offerings/create" :route.offerings/create]
        [["offerings/" :offering/address] :route.offerings/detail]
        [["offerings/" :offering/address "/edit"] :route.offerings/edit]
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
   :auction-offering/set-settings [:route.offerings/detail #(select-keys % [:offering/address])]
   :buy-now-offering-factory/create-offering [:route.ens-record/detail #(hash-map :ens.record/name (:offering/name %))]
   :auction-offering-factory/finalize [:route.offerings/detail #(select-keys % [:offering/address])]
   :buy-now-offering/buy [:route.offerings/detail #(select-keys % [:offering/address])]
   :auction-offering/withdraw [:route.offerings/detail #(select-keys % [:offering/address])]
   :auction-offering-factory/create-offering [:route.ens-record/detail #(hash-map :ens.record/name (:offering/name %))]
   :auction-offering/bid [:route.offerings/detail #(select-keys % [:offering/address])]
   :buy-now-offering/set-settings [:route.offerings/detail #(select-keys % [:offering/address])]
   :buy-now-offering/reclaim-ownership [:route.offerings/detail #(select-keys % [:offering/address])]
   :offering-requests/add-request [:route.ens-record/detail #(select-keys % [:ens.record/name])]})
