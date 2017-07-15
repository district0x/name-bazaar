(ns name-bazaar.server.effects
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.state :as state]
    [name-bazaar.contracts-api.used-by-factories :as used-by-factories])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def default-deploy-opts
  {:from-index 0
   :contracts-file-namespace 'name-bazaar.shared.smart-contracts
   :contracts-file-path "/src/shared/name_bazaar/shared/smart_contracts.cljs"})

(def library-placeholders
  {:offering-library "__OfferingLibrary.sol:OfferingLibrary___"
   :instant-buy-offering-library "__instant_buy/InstantBuyOfferingLibrar__"
   :english-auction-offering-library "__english_auction/EnglishAuctionOfferi__"})

(defn deploy-ens! [server-state-atom default-opts]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :ens})))

(defn deploy-offering-registry! [server-state-atom default-opts]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :offering-registry})))

(defn deploy-offering-requests! [server-state-atom default-opts]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :offering-requests})))

(defn deploy-offering-library! [server-state-atom default-opts]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :offering-library})))

(defn deploy-instant-buy-library! [server-state-atom default-opts]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :instant-buy-offering-library
                                                                :library-placeholders (select-keys library-placeholders
                                                                                                   [:offering-library])})))

(defn deploy-english-auction-library! [server-state-atom default-opts]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :english-auction-offering-library
                                                                :library-placeholders (select-keys library-placeholders
                                                                                                   [:offering-library])})))

(defn deploy-instant-buy-factory! [server-state-atom default-opts {:keys [:offering-factory/emergency-multisig]}]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :instant-buy-offering-factory
                                                                :library-placeholders (select-keys library-placeholders
                                                                                                   [:offering-library
                                                                                                    :instant-buy-offering-library])
                                                                :args [(state/contract-address @server-state-atom :ens)
                                                                       (state/contract-address @server-state-atom :offering-registry)
                                                                       (state/contract-address @server-state-atom :offering-requests)
                                                                       emergency-multisig]})))

(defn deploy-english-auction-factory! [server-state-atom default-opts {:keys [:offering-factory/emergency-multisig]}]
  (d0x-effects/deploy-smart-contract! server-state-atom (merge default-opts
                                                               {:contract-key :english-auction-offering-factory
                                                                :library-placeholders (select-keys library-placeholders
                                                                                                   [:offering-library
                                                                                                    :english-auction-offering-library])
                                                                :args [(state/contract-address @server-state-atom :ens)
                                                                       (state/contract-address @server-state-atom :offering-registry)
                                                                       (state/contract-address @server-state-atom :offering-requests)
                                                                       emergency-multisig]})))

(defn deploy-smart-contracts! [server-state-atom]
  (let [ch (chan)]
    (go
      (<! (deploy-ens! server-state-atom default-deploy-opts))
      (<! (deploy-offering-registry! server-state-atom default-deploy-opts))
      (<! (deploy-offering-requests! server-state-atom default-deploy-opts))
      (<! (deploy-offering-library! server-state-atom default-deploy-opts))
      (<! (deploy-instant-buy-library! server-state-atom default-deploy-opts))
      (<! (deploy-english-auction-library! server-state-atom default-deploy-opts))
      (<! (deploy-instant-buy-factory! server-state-atom default-deploy-opts {:offering-factory/emergency-multisig
                                                                              (state/active-address @server-state-atom)}))
      (<! (deploy-english-auction-factory! server-state-atom default-deploy-opts {:offering-factory/emergency-multisig
                                                                                  (state/active-address @server-state-atom)}))

      (<! (used-by-factories/set-factories! @server-state-atom {:contract-key :offering-registry}))
      (<! (used-by-factories/set-factories! @server-state-atom {:contract-key :offering-requests}))

      (d0x-effects/store-smart-contracts! (:smart-contracts @server-state-atom)
                                          {:file-path (:contracts-file-path default-deploy-opts)
                                           :namespace (:contracts-file-namespace default-deploy-opts)})

      (>! ch server-state-atom))
    ch))
