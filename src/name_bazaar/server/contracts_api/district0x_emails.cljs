(ns name-bazaar.server.contracts-api.district0x-emails
  (:require [cljs-web3.async.eth :as web3-eth-async]
            [cljs-web3.eth :as web3-eth]
            [district0x.server.state :as state]))

(defn get-email [server-state {:keys [:district0x-emails/address]}]
  (web3-eth-async/contract-call (state/instance server-state :district0x-emails) :get-email address))
