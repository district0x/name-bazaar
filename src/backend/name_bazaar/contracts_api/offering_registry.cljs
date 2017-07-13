(ns name-bazaar.contracts-api.offering-registry
  (:require [district0x.server.state :as state]
            [district0x.server.utils :as server-utils]))

(defn on-offering-added-once [server-state {:keys [:node :owner :blockchain-filter-opts]
                                            :as opts
                                            :or {:blockchain-filter-opts "latest"}}]
  (server-utils/watch-event-once (state/instance server-state :offering-registry)
                                 :on-offering-added
                                 (select-keys opts [:node :owner])
                                 blockchain-filter-opts))

(defn on-offering-added [server-state {:keys [:node :owner :blockchain-filter-opts]
                                       :as opts
                                       :or {:blockchain-filter-opts "latest"}}]
  (server-utils/watch-event-once (state/instance server-state :offering-registry)
                                 :on-offering-added
                                 (select-keys opts [:node :owner])
                                 blockchain-filter-opts))