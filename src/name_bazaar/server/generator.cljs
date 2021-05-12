(ns name-bazaar.server.generator
  (:require
    [cljs.core.async :refer [<!]]
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.utils :refer [to-wei]]
    [district.server.config :refer [config]]
    [district.server.web3 :refer [web3]]
    [district.shared.async-helpers :refer [safe-go]]
    [district0x.shared.utils :refer [rand-str rand-nth-except]]
    [name-bazaar.server.contracts-api.auction-offering :as auction-offering]
    [name-bazaar.server.contracts-api.auction-offering-factory :as auction-offering-factory]
    [name-bazaar.server.contracts-api.buy-now-offering-factory :as buy-now-offering-factory]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
    [name-bazaar.server.contracts-api.registrar :as registrar]
    [name-bazaar.server.contracts-api.buy-now-offering :as buy-now-offering]))


(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def normalize (aget (js/require "eth-ens-namehash") "normalize"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn generate [{:keys [:total-accounts :offerings-per-account :offering/type]}]
  (safe-go
    (let [my-accounts (<! (web3-eth/accounts @web3))]
      (dotimes [address-index total-accounts]
        (dotimes [_ offerings-per-account]
          (let [owner (nth my-accounts address-index)
                label (normalize (rand-str (+ (rand-int 7) 3)))
                name (str label "." registrar/root-node)
                node (namehash name)
                offering-type (or type (rand-nth [:buy-now-offering :auction-offering]))
                price (to-wei @web3 (/ (inc (rand-int 10)) 10) :ether)
                buyer (rand-nth-except owner my-accounts)
                request-name (if (zero? (rand-int 2)) name (normalize (str (rand-str 1)
                                                                           "."
                                                                           registrar/root-node)))]

            (<! (registrar/register! {:ens.record/label label} {:from owner}))

            (<! (offering-requests/add-request! {:offering-request/name request-name} {:from owner}))

            (let [receipt (if (= offering-type :buy-now-offering)
                            (<! (buy-now-offering-factory/create-offering! {:offering/name name
                                                                            :offering/price price}
                                                                           {:from owner}))
                            (<! (auction-offering-factory/create-offering!
                                  {:offering/name name
                                   :offering/price price
                                   :auction-offering/end-time (to-epoch (t/plus (t/now) (t/weeks 2)))
                                   :auction-offering/extension-duration (rand-int 10000)
                                   :auction-offering/min-bid-increase (to-wei @web3 0.1 :ether)}
                                  {:from owner})))]

              (let [{offering :offering} (<! (offering-registry/on-offering-added-in-tx receipt))]
                (<! (registrar/transfer! {:ens.record/label label :ens.record/owner offering}
                                         {:from owner}))

                (when (= offering-type :auction-offering)
                  (<! (auction-offering/bid! {:offering/address offering} {:value price :from buyer})))

                (when (zero? (rand-int 2))
                    (if (= offering-type :buy-now-offering)
                      (<! (buy-now-offering/buy! {:offering/address offering} {:value price :from buyer}))))))))))))
