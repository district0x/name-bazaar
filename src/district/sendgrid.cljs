(ns district.sendgrid
  (:require [ajax.core :as ajax :refer [POST]]))

(defonce ^:private sendgrid-public-api "https://api.sendgrid.com/v3/mail/send")

(defn send-email
  [{:keys [:from :to :subject :content :substitutions :on-success :on-error :template-id :api-key :body :headers
           :print-mode?]}]
  (if (and (not api-key)
           (not print-mode?))
    (throw (js/Error. "Missing api-key to send email to sendgrid"))
    (if print-mode?
      (do
        (println "Would send email:")
        (println "From:" from)
        (println "To:" to)
        (println "Subject:" subject)
        (println "Content:" content))
      (let [body (merge {:from {:email from}
                         :personalizations [{:to [{:email to}]
                                             ;; Substitutions are in format e.g ":header", so (str :header) works well
                                             :substitutions (into {} (map (fn [[k v]] [(str k) v]) substitutions))}]
                         :subject subject
                         :content [{:type "text/html"
                                    :value content}]
                         :template_id template-id}
                        body)]
        (POST sendgrid-public-api
              {:headers (merge {"Authorization" (str "Bearer " api-key)
                                "Content-Type" "application/json"}
                               headers)
               :body (js/JSON.stringify (clj->js body))
               :handler on-success
               :error-handler on-error})))))
