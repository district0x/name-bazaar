(ns name-bazaar.contracts-api.offering-requests
  (:require [district0x.server.state :as state]
            [cljs-web3.eth :as web3-eth]
            [district0x.server.effects :as effects]))

(defn on-request-added [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-request-added args))

(defn on-requests-cleared [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-requests) :on-requests-cleared args))

(defn add-request! [server-state {:keys [:offering-request/name]} opts]
  (effects/logged-contract-call! server-state
                                 (state/instance server-state :offering-requests)
                                 :add-request
                                 name
                                 (merge {:gas 100000
                                         :from (state/active-address server-state)}
                                        opts)))