(ns name-bazaar.server.deployer
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs.nodejs :as nodejs]
    [district.server.config.core :refer [config]]
    [district.server.smart-contracts.core :refer [contract-address deploy-smart-contract! write-smart-contracts!]]
    [district.server.web3.core :refer [web3]]
    [mount.core :as mount :refer [defstate]]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.used-by-factories :as used-by-factories]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.core :as web3]))

(declare deploy)
(defstate ^{:on-reload :noop} deployer
  :start (deploy (merge (:deployer @config)
                        (:deployer (mount/args)))))

(def namehash (aget (nodejs/require "eth-ens-namehash") "hash"))

(defn library-placeholders [& [{:keys [:emergency-multisig]}]]
  {:auction-offering "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"
   :buy-now-offering "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"
   :ens "314159265dD8dbb310642f98f50C066173C1259b"
   :offering-registry "fEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd"
   emergency-multisig "DeEDdeeDDEeDDEEdDEedDEEdDEeDdEeDDEEDDeed"})


(defn deploy-ens! [default-opts]
  (deploy-smart-contract! (merge default-opts {:contract-key :ens :gas 700000})))


(defn deploy-registrar! [default-opts]
  (deploy-smart-contract! (merge default-opts
                                 {:contract-key :registrar
                                  :gas 3200000
                                  :args [(contract-address :ens)
                                         (namehash "eth")
                                         (to-epoch (t/minus (t/now) (t/years 1)))]})))


(defn deploy-offering-registry! [default-opts {:keys [:emergency-multisig]}]
  (deploy-smart-contract! (merge default-opts
                                 {:gas 700000
                                  :contract-key :offering-registry
                                  :args [emergency-multisig]})))


(defn deploy-offering-requests! [default-opts]
  (deploy-smart-contract! (merge default-opts {:gas 1700000
                                               :contract-key :offering-requests})))


(defn deploy-buy-now-offering! [default-opts {:keys [:emergency-multisig]}]
  (deploy-smart-contract! (merge default-opts
                                 {:gas 1200000
                                  :contract-key :buy-now-offering
                                  :library-placeholders
                                  (dissoc (library-placeholders {:emergency-multisig emergency-multisig})
                                          :buy-now-offering :auction-offering)})))


(defn deploy-auction-offering! [default-opts {:keys [:emergency-multisig]}]
  (deploy-smart-contract! (merge default-opts
                                 {:gas 1700000
                                  :contract-key :auction-offering
                                  :library-placeholders
                                  (dissoc (library-placeholders {:emergency-multisig emergency-multisig})
                                          :buy-now-offering :auction-offering)})))


(defn deploy-buy-now-factory! [default-opts]
  (deploy-smart-contract! (merge default-opts
                                 {:gas 1700000
                                  :contract-key :buy-now-offering-factory
                                  :library-placeholders (select-keys (library-placeholders) [:buy-now-offering])
                                  :args [(contract-address :ens)
                                         (contract-address :offering-registry)
                                         (contract-address :offering-requests)]})))


(defn deploy-auction-factory! [default-opts]
  (deploy-smart-contract! (merge default-opts
                                 {:gas 2000000
                                  :contract-key :auction-offering-factory
                                  :library-placeholders (select-keys (library-placeholders) [:auction-offering])
                                  :args [(contract-address :ens)
                                         (contract-address :offering-registry)
                                         (contract-address :offering-requests)]})))


(defn deploy-district0x-emails! [default-opts]
  (deploy-smart-contract! (merge default-opts {:gas 500000 :contract-key :district0x-emails})))


(defn deploy [{:keys [:write? :emergency-multisig :skip-ens-registrar? :from]
               :as deploy-opts}]
  (let [emergency-multisig (or emergency-multisig (first (web3-eth/accounts @web3)))]

    (when-not skip-ens-registrar?
      (deploy-ens! deploy-opts)
      (deploy-registrar! deploy-opts)

      (ens/set-subnode-owner! {:ens.record/label "eth"
                               :ens.record/node ""
                               :ens.record/owner (contract-address :registrar)}))

    (deploy-offering-registry! deploy-opts {:emergency-multisig emergency-multisig})
    (deploy-offering-requests! deploy-opts)

    (deploy-buy-now-offering! deploy-opts {:emergency-multisig emergency-multisig})
    (deploy-buy-now-factory! deploy-opts)

    (deploy-auction-offering! deploy-opts {:emergency-multisig emergency-multisig})
    (deploy-auction-factory! deploy-opts)

    (deploy-district0x-emails! deploy-opts)

    (used-by-factories/set-factories! {:contract-key :offering-registry})
    (used-by-factories/set-factories! {:contract-key :offering-requests})

    (when write?
      (write-smart-contracts!))))

