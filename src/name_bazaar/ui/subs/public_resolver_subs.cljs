(ns name-bazaar.ui.subs.public-resolver-subs
  (:require
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :public-resolver/records
  (fn [db]
    (:public-resolver/records db)))

(reg-sub
  :public-resolver/reverse-records
  (fn [db]
    (:public-resolver/reverse-records db)))
