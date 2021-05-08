(ns name-bazaar.server.dev
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :as pprint]
    [cljs-web3-next.core :as web3-core]
    [district.server.config :refer [config]]
    [district.server.db :refer [db]]
    [district.server.endpoints]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.logging]
    [district.server.smart-contracts]
    [district.server.web3 :refer [web3]]
    [district.server.web3-events]
    [district.shared.async-helpers :as async-helpers]
    [goog.date.Date]
    [mount.core :as mount]
    [name-bazaar.server.api]
    [name-bazaar.server.db]
    [name-bazaar.server.emailer]
    [name-bazaar.server.generator :as generator]
    [name-bazaar.server.syncer]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]]
    [print.foo :include-macros true]
    [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(def namehash (aget (nodejs/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (nodejs/require "js-sha3") "keccak_256")))

(defn on-jsload []
  (mount/stop #'district.server.endpoints/endpoints)
  (mount/start #'district.server.endpoints/endpoints))

(defn generate-data
  "Generate dev data"
  []
  (let [opts (or (:generator @config)
                 {:total-accounts 1
                  :offerings-per-account 1})]
    (log/info "Generating data, please be patient..." ::generate-date)
    (generator/generate opts)))


(defn -main [& _]
  (async-helpers/extend-promises-as-channels!)
  (-> (mount/with-args
        {:config          {:default {:logging     {:level    "info"
                                                   :console? true}
                                     :endpoints   {:port        6200
                                                   :middlewares [logging-middlewares]}
                                     :web3        {:url "ws://127.0.0.1:8549"
                                                   :on-online (fn []
                                                                (log/warn "Ethereum node went online again")
                                                                (mount/start #'name-bazaar.server.db/name-bazaar-db
                                                                             #'district.server.web3-events/web3-events
                                                                             #'name-bazaar.server.syncer/syncer
                                                                             #'name-bazaar.server.emailer/emailer))
                                                   :on-offline (fn []
                                                                 (log/warn "Ethereum node went offline")
                                                                 (mount/stop #'name-bazaar.server.db/name-bazaar-db
                                                                             #'district.server.web3-events/web3-events
                                                                             #'name-bazaar.server.syncer/syncer
                                                                             #'name-bazaar.server.emailer/emailer))}
                                     :web3-events {:events {:ens/new-owner                      [:ens :NewOwner]
                                                            :ens/transfer                       [:ens :Transfer]
                                                            :offering-registry/offering-added   [:offering-registry :onOfferingAdded]
                                                            :offering-registry/offering-changed [:offering-registry :onOfferingChanged]
                                                            :offering-requests/request-added    [:offering-requests :onRequestAdded]
                                                            :offering-requests/round-changed    [:offering-requests :onRoundChanged]
                                                            :registrar/transfer                 [:eth-registrar :Transfer]}}
                                     :db          {:opts {:memory true}}
                                     :emailer     {:print-mode? true
                                                   :private-key "25677d268904ea651f84e37cfd580696c5c793dcd9730c415bf03b96003c09e9ef8"}
                                     :ui          {:public-key             "2564e15aaf9593acfdc633bd08f1fc5c089aa43972dd7e8a36d67825cd0154602da47d02f30e1f74e7e72c81ba5f0b3dd20d4d4f0cc6652a2e719a0e9d4c7f10943"
                                                   :use-instant-registrar? true
                                                   :reveal-period          {:hours 48}}}}
         :smart-contracts {:contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}})
      (mount/start)
      pprint/pprint))
