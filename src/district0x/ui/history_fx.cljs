(ns district0x.ui.history-fx
  (:require
    [district0x.ui.history :as history]
    [re-frame.core :as re-frame :refer [reg-fx reg-event-fx]]))

(reg-fx
  :history/start
  (fn [{:keys [:routes]}]
    (history/start! routes)))