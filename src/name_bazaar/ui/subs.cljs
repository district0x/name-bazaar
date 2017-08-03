(ns name-bazaar.ui.subs
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [district0x.ui.utils :refer [reg-form-sub]]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants]
    [re-frame.core :refer [reg-sub]]))

(doseq [form-key (keys constants/form-configs)]
  (reg-form-sub
    form-key
    (fn [[form]]
      form)))

