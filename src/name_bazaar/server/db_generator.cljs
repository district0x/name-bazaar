(ns name-bazaar.server.db-generator
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.core.async :refer [<! >! chan]]
    [district0x.server.state :as state]
    [district0x.server.utils :as u]
    [district0x.server.utils :refer [watch-event-once]]
    [name-bazaar.server.contracts-api.english-auction-offering :as english-auction-offering]
    [name-bazaar.server.contracts-api.english-auction-offering-factory :as english-auction-offering-factory]
    [name-bazaar.server.contracts-api.ens :as ens]
    [name-bazaar.server.contracts-api.instant-buy-offering :as instant-buy-offering]
    [name-bazaar.server.contracts-api.instant-buy-offering-factory :as instant-buy-offering-factory]
    [name-bazaar.server.contracts-api.offering-registry :as offering-registry]
    [name-bazaar.server.contracts-api.offering-requests :as offering-requests])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def namehash (aget (js/require "eth-ens-namehash") "hash"))
(def sha3 (comp (partial str "0x") (aget (js/require "js-sha3") "keccak_256")))

(defn generate! [server-state {:keys [:total-accounts]}]
  (let [ch (chan)
        root-node "eth"]
    (go
      (<! (ens/set-subnode-owner! server-state {:ens.record/label root-node
                                                :ens.record/owner (state/active-address server-state)}))

      (doseq [address-index (range total-accounts)]
        (let [owner (state/my-address server-state address-index)
              label (u/rand-str 5 {:lowercase-only? true})
              name (str label "." root-node)
              node (namehash name)
              offering-type (if (zero? (rand-int 2)) :instant-buy-offering :english-auction-offering)
              price (web3/to-wei (inc (rand-int 10)) :ether)
              buyer (state/my-address server-state (rand-int total-accounts))
              request-name (if (zero? (rand-int 2)) name (str (u/rand-str 1 {:lowercase-only? true}) "." root-node))]
          (<! (ens/set-subnode-owner! server-state
                                      {:ens.record/label label
                                       :ens.record/node root-node
                                       :ens.record/owner owner}
                                      {:from (state/active-address server-state)}))

          (<! (offering-requests/add-request! server-state {:offering-request/name request-name} {:form owner}))

          (if (= offering-type :instant-buy-offering)
            (<! (instant-buy-offering-factory/create-offering! server-state
                                                               {:offering/name name
                                                                :offering/price price}
                                                               {:from owner}))
            (<! (english-auction-offering-factory/create-offering!
                  server-state
                  {:offering/name name
                   :offering/price price
                   :english-auction-offering/end-time (to-epoch (t/plus (t/now) (t/weeks 2)))
                   :english-auction-offering/extension-duration (rand-int 10000)
                   :english-auction-offering/min-bid-increase (web3/to-wei 1 :ether)}
                  {:from owner})))


          (let [[_ {{:keys [:offering]} :args}] (<! (offering-registry/on-offering-added-once server-state
                                                                                              {:node node
                                                                                               :owner owner}))]
            (<! (ens/set-owner! server-state {:ens.record/node node :ens.record/owner offering} {:from owner}))

            (when (zero? (rand-int 2))
              (if (= offering-type :instant-buy-offering)
                (instant-buy-offering/buy! server-state {:contract-address offering
                                                         :value price
                                                         :from buyer})
                (english-auction-offering/bid! server-state {:contract-address offering
                                                             :value price
                                                             :from buyer})))

            )))




      #_(<! (instant-buy-offering/buy! server-state {:contract-address offering
                                                     :value-ether 0.01
                                                     :from (state/my-address server-state 1)}))


      #_(let [[_ {{:keys [:offering]} :args}]
              (<! (offering-registry/on-offering-added-once server-state
                                                            {:node (namehash "eth")
                                                             :owner (state/active-address server-state)}))]
          (<! (ens/set-owner! server-state {:ens.record/name "eth" :ens.record/owner offering}))

          (<! (instant-buy-offering/buy! server-state {:contract-address offering
                                                       :value-ether 0.01
                                                       :from (state/my-address server-state 1)}))

          )



      (>! ch true))
    ch))
