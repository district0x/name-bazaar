(ns name-bazaar.ui.components.app-bar-search
  (:require
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [ensure-registrar-root valid-ens-name? normalize]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn- nav-to-ens-record-detail [name]
  (when-not (empty? name)
    (dispatch [:district0x.location/nav-to :route.ens-record/detail
               {:ens.record/name (normalize (ensure-registrar-root name))}
               constants/routes])))

(defn app-bar-search []
  (let [searched-name (r/atom "")]
    (fn [props]
      [:div.app-bar-search-wrapper
       (r/merge-props
         {:style styles/app-bar-search-wrapper}
         props)
       [:div
        {:style styles/app-bar-search-icon
         :on-click (fn []
                     (nav-to-ens-record-detail @searched-name)
                     (reset! searched-name ""))}
        (icons/magnify
          {:color "#FFF"})]
       [:input
        {:style styles/app-bar-search-input
         :value @searched-name
         :on-key-press (fn [e]
                         (when (= (aget e "key") "Enter")
                           (nav-to-ens-record-detail @searched-name)
                           (reset! searched-name "")))
         :on-change (fn [e]
                      (let [value (aget e "target" "value")]
                        (when (valid-ens-name? value)
                          (reset! searched-name value))))}]])))
