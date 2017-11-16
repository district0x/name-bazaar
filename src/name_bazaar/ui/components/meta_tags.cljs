(ns name-bazaar.ui.components.meta-tags
  (:require [district0x.ui.components.meta-tags :as d0x-meta-tags]
            [re-frame.core :refer [subscribe]]))

(defn meta-tags [{:keys [:title :description]
                              :or {title "Name Bazaar"
                                   description "A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service."}}]
  (let [{:keys [:handler :query-params :path] :as active-page} @(subscribe [:district0x/active-page])]
    [d0x-meta-tags/meta-tags {:id "name-bazaar-meta-tags"}
     [:title {:id "title" :title title}]
     [:meta {:id "description" :content description}]
     [:meta {:id "url" :name "og:url" :content (str "https://namebazaar.io" path)}]]))
