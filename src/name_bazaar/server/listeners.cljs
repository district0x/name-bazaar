(ns name-bazaar.server.listeners
  (:require [cljs-web3.eth :as web3-eth]
            [cljs.core.async :refer [<! >! chan]]
            [district0x.server.state :as state]
            [name-bazaar.server.contracts-api.district0x-emails :as district0x-emails]
            [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
            [district0x.server.sendgrid :as sendgrid]
            [district0x.shared.config :as config]
            [district0x.shared.key-utils :as key-utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- get-instance [server-state contract-key]
  (get-in @server-state [:smart-contracts contract-key :instance]))

(defn on-offering-added [server-state {:keys [:offering :node :owner :version] :as args}]
  (go
    (let [[error success] (<! (offering-requests/get-request @server-state {:offering-request/node node}))]
      (if error
        (prn "An error has occured: " error)
        (let [{:keys [name :offering-request/name
                      requesters-count :offering-request/requesters-count
                      latest-round :offering-request/latest-round
                      node :offering-request/node]} success]
          (when (pos? requesters-count)
            (let [[_ addresses] (<! (offering-requests/get-requesters @server-state {:offering-request/node node
                                                                                     :offering-request/round latest-round}))]
              (for [address addresses]
                (let [[_ base64-encrypted-email] (<! (district0x-emails/get-email @server-state {:district0x-emails/address address}))]
                  (sendgrid/send-notification-email {:from-email "test@test.com"
                                                      :to-email (->> base64-encrypted-email
                                                                     (key-utils/decode-base64)
                                                                     (key-utils/decrypt (config/get-config :PRIVATE_KEY)))
                                                      :subject "Subject"
                                                      :content "content"}
                                                     #(prn "Success sending email")
                                                     #(prn "An error has occured: " %)))))))))))

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



