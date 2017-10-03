(ns district0x.server.emailer.sendgrid
  (:require [ajax.core :as ajax :refer [POST]]
            [district0x.shared.config :as config]
            [district0x.server.utils :as server-utils]))

(def ^private sendgrid-public-api "https://api.sendgrid.com/v3")

(defn send-notification-email
  [{:keys [from-email to-email subject content]} success-handler error-handler]
  (let [body {:from {:email from-email}
              :personalizations [{:to [{:email to-email}]}]
              :subject subject
              :content [{:type "text/html"
                         :value content}]}]
    (POST (str sendgrid-public-api "/mail/send")
          {:headers {"Authorization" (str "Bearer " (config/get-config :sendgrid-api-key))
                     "Content-type" "application/json"}
           :body (server-utils/clj->json body)}
          :handler success-handler
          :error-handler error-handler)))

(comment
  (district0x.server.sendgrid/send-notification-email {:from-email "test@test.com"
                                                       :to-email "zveyfovk@sharklasers.com"
                                                       :subject "Subject"
                                                       :content "content"}
                                                      #(prn "Success!")
                                                      #(prn "Error: " %)))



