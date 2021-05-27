(ns name-bazaar.ui.components.main-panel
  (:require
    [clojure.set :as set]
    [district0x.ui.components.active-address-select :refer [active-address-select]]
    [district0x.ui.components.misc :refer [page]]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.pages.about-page]
    [name-bazaar.ui.pages.ens-record-detail-page]
    [name-bazaar.ui.pages.home-page]
    [name-bazaar.ui.pages.how-it-works-page]
    [name-bazaar.ui.pages.instant-register-name-page]
    [name-bazaar.ui.pages.my-settings-page]
    [name-bazaar.ui.pages.offering-create-page]
    [name-bazaar.ui.pages.offering-detail-page]
    [name-bazaar.ui.pages.offerings-search-page]
    [name-bazaar.ui.pages.user-bids-page]
    [name-bazaar.ui.pages.user-offerings-page]
    [name-bazaar.ui.pages.user-purchases-page]
    [name-bazaar.ui.pages.watched-names-page]
    [name-bazaar.ui.pages.manage-names-page]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn main-panel []
  (let [active-page (subscribe [:district0x/active-page])]
    (fn []
      (let [{:keys [:handler]} @active-page]
        ^{:key (str handler)} [page handler]))))
