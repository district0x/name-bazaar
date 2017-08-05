(ns name-bazaar.ui.components.main-panel
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [district0x.ui.components.active-address-select-field :refer [active-address-select-field]]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col center-layout paper]]
    [medley.core :as medley]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.pages.about-page :refer [about-page]]
    [name-bazaar.ui.pages.ens-record-detail-page :refer [ens-record-detail-page]]
    [name-bazaar.ui.pages.home-page :refer [home-page]]
    [name-bazaar.ui.pages.how-it-works-page :refer [how-it-works-page]]
    [name-bazaar.ui.pages.my-offerings-page :refer [my-offerings-page]]
    [name-bazaar.ui.pages.my-settings-page :refer [my-settings-page]]
    [name-bazaar.ui.pages.offering-create-page :refer [offering-create-page]]
    [name-bazaar.ui.pages.offering-detail-page :refer [offering-detail-page]]
    [name-bazaar.ui.pages.offering-requests-search-page :refer [offering-requests-search-page]]
    [name-bazaar.ui.pages.offerings-search-page :refer [offerings-search-page]]
    [name-bazaar.ui.pages.user-offerings-page :refer [user-offerings-page]]
    [name-bazaar.ui.pages.watched-ens-records-page :refer [watched-ens-records-page]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def route->component
  {:route.ens-record/detail about-page
   :route/watched-ens-records watched-ens-records-page
   :route.user/offerings user-offerings-page
   :route.user/my-settings my-settings-page
   :route.user/my-offerings my-offerings-page
   :route.offering/create offering-create-page
   :route.offering/detail offering-detail-page
   :route.offerings/search offerings-search-page
   :route.offering-requests/search offering-requests-search-page
   :route/about about-page
   :route/how-it-works how-it-works-page
   :route/home home-page})

(defn main-panel []
  (let [xs-width? (subscribe [:district0x/window-xs-width?])
        active-page (subscribe [:district0x/active-page])]
    (fn []
      (let [{:keys [:handler]} @active-page]
        [misc/main-panel
         {:mui-theme styles/mui-theme}
         (if (= :home handler)
           (route->component handler)
           )]))))
