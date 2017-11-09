(ns name-bazaar.server.contracts-api.auction-offering-factory
  (:require
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]))

(defn create-offering! [args opts]
  (apply logged-contract-call! (state/instance :auction-offering-factory)
         :create-offering
         (concat
           ((juxt :offering/name
                  :offering/price
                  :auction-offering/end-time
                  :auction-offering/extension-duration
                  :auction-offering/min-bid-increase)
             args)
           [(merge {:gas 1000000
                    :from (state/active-address)}
                   opts)])))


(defn ens []
  (queue-contract-call! (state/instance :auction-offering-factory) :ens))

(defn root-node []
  (queue-contract-call! (state/instance :auction-offering-factory) :root-node))

(defn offering-registry []
  (queue-contract-call! (state/instance :auction-offering-factory) :offering-registry))

(defn offering-requests []
  (queue-contract-call! (state/instance :auction-offering-factory) :offering-requests))
