(ns name-bazaar.ui.events.public-resolver-events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address? merge-in]]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 parse-query-params path-for get-ens-record-name get-offering-name get-offering]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [district0x.shared.utils :as d0x-shared-utils]
    [medley.core :as medley]
    [taoensso.timbre :as logging :refer-macros [info warn error]]
    ))

(reg-event-fx
 :public-resolver.record.addr/load
 interceptors
 (fn [{:keys [:db]} [resolver node]]
   (let [instance (get-instance db :public-resolver resolver)]
     (info [:RESOLVING-NODE node resolver instance])
     {:web3-fx.contract/constant-fns
      {:fns
       [{:instance instance
         :method :addr
         :args [node]
         :on-success [:public-resolver.record.addr/loaded resolver node]
         :on-error [:district0x.log/error]}]}})))

(reg-event-fx
 :public-resolver.record.addr/loaded
 interceptors
 (fn [{:keys [:db]} [resolver node addr]]
   (.log js/console ["Resolved node" node addr])
   {:db (if (or
             (not resolver)
             (not node)
             (not addr)
             (= addr "0x"))
          db
          (assoc-in db [:public-resolvers
                        resolver
                        :public-resolver/records
                        node
                        :public-resolver.record/addr] (if (= addr "0x")
                                                        d0x-shared-utils/zero-address
                                                        addr)))}))
