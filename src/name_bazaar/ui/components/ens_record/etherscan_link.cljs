(ns name-bazaar.ui.components.ens-record.etherscan-link
  (:require
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [etherscan-ens-url]]))

(defn ens-record-etherscan-link [{:keys [:ens.record/name]}]
  [:div
   [:a
    {:href (etherscan-ens-url name)
     :target :_blank
     :style styles/text-decor-none}
    "Open in Etherscan"]])
