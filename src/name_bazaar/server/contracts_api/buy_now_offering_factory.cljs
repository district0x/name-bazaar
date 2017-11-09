(ns name-bazaar.server.contracts-api.buy-now-offering-factory
  (:require
    [cljs-web3.async.eth :as web3-eth]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]))

(defn create-offering! [{:keys [:offering/name :offering/price]} opts]
  (logged-contract-call! (state/instance :buy-now-offering-factory)
                         :create-offering
                         name
                         price
                         (merge {:gas 1000000
                                 :from (state/active-address)}
                                opts)))

(defn ens []
  (queue-contract-call! (state/instance :buy-now-offering-factory) :ens))

(defn root-node []
  (queue-contract-call! (state/instance :buy-now-offering-factory) :root-node))

(defn offering-registry []
  (queue-contract-call! (state/instance :buy-now-offering-factory) :offering-registry))

(defn offering-requests []
  (queue-contract-call! (state/instance :buy-now-offering-factory) :offering-requests))
