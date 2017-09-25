(ns district0x.server.sendgrid
  (:require
   [ajax.core :as ajax :refer [POST]]
   [district0x.shared.config :as config]))

(def ^private sendgrid-public-api "https://api.sendgrid.com/v3")

(defn clj->json
  [coll]
  (.stringify js/JSON (clj->js coll)))

(defn send-notification-email
  [{:keys [from-email to-email subject content]} success-handler error-handler]
  (let [body {:from {:email from-email}
              :personalizations [{:to [{:email to-email}]}]
              :subject subject
              :content [{:type "text/html"
                         :value content}]}]
    (POST (str sendgrid-public-api "/mail/send")
          {:headers {"Authorization" (str "Bearer " (config/get-config :SENDGRID_API_KEY))
                     "Content-type" "application/json"}
           :body (clj->json body)}
           :handler success-handler
           :error-handler error-handler)))

(comment
  (district0x.server.sendgrid/send-notification-email {:from-email "test@test.com"
                                                       :to-email "zveyfovk@sharklasers.com"
                                                       :subject "Subject"
                                                       :content "content"}
                                                      #(prn "Success!")
                                                      #(prn "Error: " %)))



