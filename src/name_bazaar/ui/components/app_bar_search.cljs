(ns name-bazaar.ui.components.app-bar-search
  (:require
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [ensure-registrar-root valid-ens-name? normalize]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn- nav-to-ens-record-detail [name]
  (when-not (empty? name)
    (dispatch [:district0x.location/nav-to :route.ens-record/detail
               {:ens.record/name (normalize (ensure-registrar-root name))}
               constants/routes])))

(defn app-bar-search []
  (let [searched-name (r/atom "")]
    (fn [props]
      [:div.app-bar-search-container
       props
       [ui/Input
        {:value @searched-name
         :on-key-press (fn [e]
                         (when (= (aget e "key") "Enter")
                           (nav-to-ens-record-detail @searched-name)
                           (reset! searched-name "")))
         :icon (r/as-element [:i.icon.magnifier2
                              {:on-click (fn []
                                           (nav-to-ens-record-detail @searched-name)
                                           (reset! searched-name ""))}])
         :on-change (fn [e]
                      (let [value (aget e "target" "value")]
                        (when (valid-ens-name? value)
                          (reset! searched-name value))))}]])))
