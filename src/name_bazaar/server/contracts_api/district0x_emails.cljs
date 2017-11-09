(ns name-bazaar.server.contracts-api.district0x-emails
  (:require
    [cljs-web3.eth :as web3-eth]
    [district0x.server.state :as state]
    [district0x.server.effects :refer [logged-contract-call! queue-contract-call!]]))

(defn get-email [{:keys [:district0x-emails/address]}]
  (queue-contract-call! (state/instance :district0x-emails) :get-email address))
