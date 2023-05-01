(ns district.ui.logging.events
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
 ::info
 [interceptors]
 (fn [_ args]
   {:log/info args}))

(re-frame/reg-fx
 :log/info
 (fn [[message meta ns]]
   (log/info message meta ns)))

(re-frame/reg-event-fx
 ::warn
 [interceptors]
 (fn [_ args]
   {:log/warn args}))

(re-frame/reg-fx
 :log/warn
 (fn [[message meta ns]]
   (log/warn message meta ns)))

(re-frame/reg-event-fx
 ::error
 [interceptors]
 (fn [_ args]
   {:log/error args}))

(re-frame/reg-fx
 :log/error
 (fn [[message meta ns]]
   (if (string? message)
     (log/error message meta ns)
     (log/error (ex-message message) {:error message}))))
