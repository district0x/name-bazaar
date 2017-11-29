(ns name-bazaar.ui.events.public-resolver-events
  (:require
    [cljs-web3.core :as web3]
    [cljs.spec.alpha :as s]
    [clojure.set :as set]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils :refer [eth->wei empty-address? merge-in]]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.events :refer [get-contract get-instance get-instance reg-empty-event-fx]]
    [district0x.ui.spec-interceptors :refer [validate-args conform-args validate-db validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [namehash sha3 parse-query-params path-for get-ens-record-name get-offering-name get-offering]]
    [re-frame.core :as re-frame :refer [reg-event-fx inject-cofx path after dispatch trim-v console]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(defn reverse-record-node [addr]
  (namehash (str (apply str (drop 2 addr))
                 ".addr.reverse")))

(reg-event-fx
 :public-resolver.addr/load
 interceptors
 (fn [{:keys [:db]} [node]]
   (let [instance (get-instance db :public-resolver)]
     (info [:RESOLVING-NODE node instance])
     {:web3-fx.contract/constant-fns
      {:fns
       [{:instance instance
         :method :addr
         :args [node]
         :on-success [:public-resolver.addr/loaded node]
         :on-error [:district0x.log/error]}]}})))

(reg-event-fx
 :public-resolver.addr/loaded
 interceptors
 (fn [{:keys [:db]} [node addr]]
   (info [:RESOLVED-NODE node addr])
   (when-not (empty-address? addr)
     {:db (assoc-in db [:public-resolver/records node :public-resolver.record/addr] addr)})))

(reg-event-fx
 :public-resolver.name/load
 interceptors
 (fn [{:keys [:db]} [addr]]
   (when (and (not (empty-address? addr))
              (web3/address? addr))
     (let [node (reverse-record-node addr)
           instance (get-instance db :public-resolver)]
       (info [:REVERSE-RESOLVING-ADDR addr node instance])
       {:web3-fx.contract/constant-fns
        {:fns
         [{:instance instance
           :method :name
           :args [node]
           :on-success [:public-resolver.name/loaded addr]
           :on-error [:district0x.log/error]}]}}))))

(reg-event-fx
 :public-resolver.name/loaded
 interceptors
 (fn [{:keys [:db]} [addr name]]
   (info [:REVERSE-RESOLVED-ADDR addr name])
   (when (and name
              (not= name ""))
     {:db (assoc-in db [:public-resolver/reverse-records
                        addr
                        :public-resolver.record/name] name)})))

(reg-event-fx
 :public-resolver.name/point
 interceptors
 (fn [{:keys [:db]} [name addr]]
   (let [node (namehash name)
         instance (get-instance db :public-resolver)]
     (info [:NAME-POINT addr node instance])
     {:web3-fx.contract/constant-fns
      {:fns
       [{:instance instance
         :method :setAddr
         :args [node addr]
         :on-success [:public-resolver.name/point-completed name addr]
         :on-error [:district0x.log/error]}]}})))

(reg-event-fx
 :public-resolver.name/point-completed
 interceptors
 (fn [{:keys [:db]} [name addr]]
   (info [:NAME-POINTED name addr])
   nil))
