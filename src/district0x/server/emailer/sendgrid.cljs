(ns district0x.server.emailer.sendgrid
  (:require [ajax.core :as ajax :refer [POST]]
            [district0x.server.state :as state]
            [district0x.server.utils :as server-utils]))

(defonce ^private sendgrid-public-api "https://api.sendgrid.com/v3")

(defn send-notification-email
  [{:keys [from-email to-email subject content]}
   {:keys [header button-title button-href] :as substitutions}
   success-handler
   error-handler]
  (let [body {:from {:email from-email}
              :personalizations [{:to [{:email to-email}]
                                  :substitutions {":header" header
                                                  ":button-title" button-title
                                                  ":button-href" button-href}}]
              :subject subject
              :content [{:type "text/html"
                         :value content}]
              :template_id "93c0f083-1fcc-4a47-ae7c-2c8aef50c3ea"}]
    (POST (str sendgrid-public-api "/mail/send")
          {:headers {"Authorization" (str "Bearer " (state/config :sendgrid-api-key))
                     "Content-type" "application/json"}
           :body (server-utils/clj->json body)}
          :handler success-handler
          :error-handler error-handler)))
