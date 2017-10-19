(ns name-bazaar.server.contracts-api.offering-registry
  (:require
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.async.eth :as web3-eth-async]
    [district0x.server.effects :as effects]
    [district0x.server.state :as state]
    [district0x.server.utils :as d0x-server-utils]))

(defn on-offering-added-once [server-state & args]
  (apply d0x-server-utils/watch-event-once (state/instance server-state :offering-registry) :on-offering-added args))

(defn on-offering-added [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-registry) :on-offering-added args))

(defn on-offering-changed [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-registry) :on-offering-changed args))

(defn on-offering-bid [server-state & args]
  (apply web3-eth/contract-call (state/instance server-state :offering-registry) :on-offering-bid args))


(defn emergency-pause! [server-state opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :offering-registry))
                                                             (state/contract-address server-state :offering-registry))
                                 :emergencyPause
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))

(defn emergency-release! [server-state opts]
  (effects/logged-contract-call! server-state
                                 (web3-eth-async/contract-at (state/web3 server-state)
                                                             (:abi (state/contract
                                                                    server-state
                                                                    :offering-registry))
                                                             (state/contract-address server-state :offering-registry))
                                 :emergencyRelease
                                 (merge {:gas 300000
                                         :from (state/active-address server-state)}
                                        opts)))
