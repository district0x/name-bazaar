(ns name-bazaar.server.listeners
  (:require [cljs-web3.eth :as web3-eth]
            [cljs.core.async :refer [<! >! chan]]
            [district0x.server.state :as state]
            [name-bazaar.server.contracts-api.offering-requests :as offering-requests])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; https://medium.com/@theMadKing/watching-solidity-events-the-right-way-d3d0a30bdc4d

(defn on-offering-added [server-state {:keys [:offering :node :owner :version] :as args}]
  (prn "@on-offering-added: ")
   (go
    (let [request (<! (offering-requests/get-request server-state {:offering-request/node node}))]

        (prn "req:" request)

      ))  
  )

(defn setup-listener! [server-state contract-key event-key callback]
  (web3-eth/contract-call (state/instance @server-state contract-key)
                          event-key
                          {}
                          "latest"
                          (fn [error {:keys [:args] :as response}]
                            (if error
                              (prn "An error has occured: " error)
                              (callback server-state args)))))

(defn setup-listeners! [server-state]
  (setup-listener! server-state :offering-registry :on-offering-added on-offering-added))




#_(name-bazaar.server.contracts-api.offering-requests/add-request!
   district0x.server.state/*server-state*
   {:offering-request/name "test-request-name"}
   {:from (district0x.server.state/my-address 0)})



