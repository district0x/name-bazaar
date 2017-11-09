(ns name-bazaar.server.contracts-api.offering-registry
  (:require
    [cljs-web3.eth :as web3-eth]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]))

(defn on-offering-added-once [& args]
  (apply d0x-server-utils/watch-event-once (state/instance :offering-registry) :on-offering-added args))

(defn on-offering-added [& args]
  (apply web3-eth/contract-call (state/instance :offering-registry) :on-offering-added args))

(defn on-offering-changed [& args]
  (apply web3-eth/contract-call (state/instance :offering-registry) :on-offering-changed args))

(defn on-offering-bid [& args]
  (apply web3-eth/contract-call (state/instance :offering-registry) :on-offering-bid args))


(defn emergency-pause! [opts]
  (logged-contract-call! (web3-eth/contract-at (state/web3)
                                               (:abi (state/contract :offering-registry))
                                               (state/contract-address :offering-registry))
                         :emergency-pause
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))

(defn emergency-release! [opts]
  (logged-contract-call! (web3-eth/contract-at (state/web3)
                                               (:abi (state/contract :offering-registry))
                                               (state/contract-address :offering-registry))
                         :emergency-release
                         (merge {:gas 300000
                                 :from (state/active-address)}
                                opts)))
