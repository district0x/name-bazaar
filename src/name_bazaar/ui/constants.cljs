(ns name-bazaar.ui.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [collify]]
    [district0x.ui.spec-interceptors :refer [validate-db]]
    [district0x.ui.utils :refer [parse-boolean-string parse-kw-coll-query-params parse-int-or-nil parse-float-or-nil]]
    [medley.core :as medley]
    [re-frame.core :as re-frame :refer [trim-v]]))

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

(def interceptors [trim-v (validate-db :name-bazaar.ui.db/db)])
