(ns district0x.ui.components.misc
  (:require
    [clojure.set :as set]
    [district0x.shared.utils :refer [empty-address?]]
    [district0x.ui.utils :refer [parse-props-children etherscan-url]]
    [re-frame.core :as re-frame]
    [reagent.core :as r]
    [medley.core :as medley]))

(defmulti page identity)

(defn etherscan-link [props & children]
  (let [root-url @(re-frame/subscribe [:district0x/config :etherscan-url])
        [{:keys [:address :transaction?] :as props} children] (parse-props-children props children)]
    (if (empty-address? address)
      [:span (if children children address)]
      [:a (r/merge-props
            {:href (etherscan-url (or root-url "https://etherscan.io")
                                  address
                                  {:type (if transaction?
                                           :transaction
                                           :address)})
             :target :_blank}
            (dissoc props :address :tx-hash :transaction?))
       (if children children address)])))

(defn youtube [props]
  [:iframe
   (r/merge-props
     {:class "youtube"
      :width 560
      :height 315
      :frameBorder 0
      :allowFullScreen true}
     props)])
