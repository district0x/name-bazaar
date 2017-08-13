(ns name-bazaar.server.contracts-api.offering-registry
  (:require
    [cljs-web3.eth :as web3-eth]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]))

(defn on-offering-added-once [server-state & args]
  (apply d0x-server-utils/watch-event-once (state/instance server-state :offering-registry) :on-offering-added args))

(defn on-offering-added [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-registry) :on-offering-added args))

(defn on-offering-changed [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-registry) :on-offering-changed args))