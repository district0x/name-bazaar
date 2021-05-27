(ns name-bazaar.ui.constants
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [collify]]
    [district0x.ui.spec-interceptors :refer [validate-db]]
    [district0x.ui.utils :refer [parse-boolean-string parse-kw-coll-query-params parse-int-or-nil parse-float-or-nil str-keyword->keyword]]
    [medley.core :as medley]
    [name-bazaar.shared.constants :as shared-constants]
    [re-frame.core :as re-frame :refer [trim-v]]))

(def contracts-version "2")
(def registrar-root ".eth")

(def default-gas-price 5000000000)

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
                            :order-by-dir str-keyword->keyword
                            :order-by str-keyword->keyword
                            :sold? parse-boolean-string
                            :offset parse-int-or-nil}})

(def routes shared-constants/routes)

(def interceptors [trim-v (validate-db :name-bazaar.ui.db/db)])

(defn infinite-list-collapsed-item-height [mobile?]
  (if mobile? 70 54))

(def coinbase
  {:main-mobile-link "https://wallet.coinbase.com/"
   :android-mobile-link "https://play.google.com/store/apps/details?id=org.toshi"
   :ios-mobile-link "https://itunes.apple.com/app/coinbase-wallet/id1278383455?ls=1&mt=8"})
