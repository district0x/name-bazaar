(ns name-bazaar.ui.components.main-panel
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [district0x.ui.components.active-address-select-field :refer [active-address-select-field]]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.pages.about-page]
    [name-bazaar.ui.pages.ens-record-detail-page]
    [name-bazaar.ui.pages.home-page]
    [name-bazaar.ui.pages.how-it-works-page]
    [name-bazaar.ui.pages.my-settings-page]
    [name-bazaar.ui.pages.offering-create-page]
    [name-bazaar.ui.pages.offering-detail-page]
    [name-bazaar.ui.pages.offering-requests-search-page]
    [name-bazaar.ui.pages.offerings-search-page]
    [name-bazaar.ui.pages.user-bids-page]
    [name-bazaar.ui.pages.user-offerings-page]
    [name-bazaar.ui.pages.user-purchases-page]
    [name-bazaar.ui.pages.watched-names-page]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn main-panel []
  (let [active-page (subscribe [:district0x/active-page])]
    (fn []
      (let [{:keys [:handler]} @active-page]
        [misc/main-panel
         {:mui-theme styles/mui-theme}
         ^{:key handler} [page handler]]))))
