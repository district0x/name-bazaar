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
                  :offerings-per-account 10})]
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
                                                            :offering-registry/offering-added   [:offering-registry :on-offering-added]
                                                            :offering-registry/offering-changed [:offering-registry :on-offering-changed]
                                                            :registrar/transfer                 [:eth-registrar :Transfer]}}
                                     :db          {:opts {:memory true}}
                                     :emailer     {:print-mode? true
                                                   :private-key "1925a0d3085e4d43a577b1adcaed60c08ece1570a151988dc41"}
                                     :ui          {:public-key             "192eb918a8a9996cf0233023b4d6b8d8071b7df392535ef72622136569abd4b8c009f302d9884d4ea54fd4714764fb44387"
                                                   :use-instant-registrar? true
                                                   :reveal-period          {:hours 48}}}}
         :smart-contracts {:contracts-build-path "./resources/public/contracts-build/"
                           :contracts-var #'name-bazaar.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}})
      (mount/start)
      pprint/pprint))
