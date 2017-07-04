(ns name-bazaar.components.main-panel
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [district0x.components.active-address-select-field :refer [active-address-select-field]]
    [district0x.components.misc :as misc :refer [row row-with-cols col center-layout paper]]
    [district0x.utils :as u]
    [medley.core :as medley]
    [name-bazaar.constants :as constants]
    [name-bazaar.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn main-panel []
  (let [connection-error? (subscribe [:district0x/blockchain-connection-error?])
        xs-width? (subscribe [:district0x/window-xs-width?])]
    (fn []
      [misc/main-panel
       {:mui-theme styles/mui-theme}
       [:div "hello"]])))
