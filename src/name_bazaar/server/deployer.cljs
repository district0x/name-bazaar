(ns name-bazaar.server.deployer
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.eth :as web3-eth]
    [cljs.nodejs :as nodejs]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address deploy-smart-contract! write-smart-contracts!]]
    [district.server.web3 :refer [web3]]
    [mount.core :as mount :refer [defstate]]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.used-by-factories :as used-by-factories]
    [clojure.string :as string]))

(declare deploy)
(defstate ^{:on-reload :noop} deployer
          :start (deploy (merge (:deployer @config)
                                (:deployer (mount/args)))))

(def namehash (aget (nodejs/require "eth-ens-namehash") "hash"))


(def emergency-multisig-placeholder (string/lower-case "DeEDdeeDDEeDDEEdDEedDEEdDEeDdEeDDEEDDeed") )
(def offering-placeholder (string/lower-case "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef") )
(def ens-placeholder (string/lower-case "314159265dD8dbb310642f98f50C066173C1259b") )
(def offering-registry-placeholder (string/lower-case "fEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd") )


(defn deploy-ens! [default-opts]
  (deploy-smart-contract! :ens (merge default-opts {:gas 700000})))


(defn deploy-registrar! [default-opts]
  (deploy-smart-contract! :name-bazaar-registrar (merge default-opts
                                                        {:gas 3200000
                                                         :arguments [(contract-address :ens)
                                                                     (namehash "eth")
                                                                     (to-epoch (t/minus (t/now) (t/years 1)))]})))


(defn deploy-offering-registry! [default-opts {:keys [:emergency-multisig]}]
  (deploy-smart-contract! :offering-registry (merge default-opts
                                                    {:gas 700000
                                                     :arguments [emergency-multisig]})))


(defn deploy-offering-requests! [default-opts]
  (deploy-smart-contract! :offering-requests (merge default-opts {:gas 1700000})))


(defn deploy-buy-now-offering! [default-opts {:keys [:emergency-multisig]}]
  (deploy-smart-contract! :buy-now-offering (merge default-opts
                                                   {:gas 1200000
                                                    :placeholder-replacements
                                                    {emergency-multisig-placeholder emergency-multisig
                                                     ens-placeholder :ens
                                                     offering-registry-placeholder :offering-registry}})))


(defn deploy-auction-offering! [default-opts {:keys [:emergency-multisig]}]
  (deploy-smart-contract! :auction-offering (merge default-opts
                                                   {:gas 1700000
                                                    :placeholder-replacements
                                                    {emergency-multisig-placeholder emergency-multisig
                                                     ens-placeholder :ens
                                                     offering-registry-placeholder :offering-registry}})))


(defn deploy-buy-now-factory! [default-opts]
  (deploy-smart-contract! :buy-now-offering-factory
                          (merge default-opts
                                 {:gas 1700000
                                  :placeholder-replacements {offering-placeholder :buy-now-offering}
                                  :arguments [(contract-address :ens)
                                              (contract-address :offering-registry)
                                              (contract-address :offering-requests)]})))


(defn deploy-auction-factory! [default-opts]
  (deploy-smart-contract! :auction-offering-factory (merge default-opts
                                                           {:gas 2000000
                                                            :placeholder-replacements {offering-placeholder :auction-offering}
                                                            :arguments [(contract-address :ens)
                                                                        (contract-address :offering-registry)
                                                                        (contract-address :offering-requests)]})))


(defn deploy-district0x-emails! [default-opts]
  (deploy-smart-contract! :district0x-emails (merge default-opts {:gas 500000})))


(defn deploy [{:keys [:write? :emergency-multisig :skip-ens-registrar?]
               :as deploy-opts}]
  (let [emergency-multisig (or emergency-multisig (first (web3-eth/accounts @web3)))]

    (when-not skip-ens-registrar?
      (deploy-ens! deploy-opts)
      (deploy-registrar! deploy-opts)

      (ens/set-subnode-owner! {:ens.record/label "eth"
                               :ens.record/node ""
                               :ens.record/owner (contract-address :name-bazaar-registrar)}))

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

