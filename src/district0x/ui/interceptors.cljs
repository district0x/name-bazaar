(ns district0x.ui.interceptors
  (:require [re-frame.core :as re-frame]))

(defn inject-sub
  [query k]
  (re-frame/->interceptor
   :id :inject-sub
   :before (fn [context]
             (let [{:keys [:db _]} (:coeffects context)]
               (assoc-in context [:coeffects k] (get-in db query))))))
