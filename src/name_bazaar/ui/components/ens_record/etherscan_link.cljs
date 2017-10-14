(ns name-bazaar.ui.components.ens-record.etherscan-link
  (:require
    [name-bazaar.ui.utils :refer [etherscan-ens-url]]))

(defn ens-record-etherscan-link [{:keys [:ens.record/name]}]
  [:div
   [:a.no-decor
    {:href (etherscan-ens-url name)
     :target :_blank}
    "Open in Etherscan"]])
