(ns district0x.components.pagination
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljsjs.react-ultimate-pagination]
    [reagent.core :as r]))

(def button-style {:min-width 36})

(defn create-config [{:keys [:style :first-page-icon :previous-page-icon :next-page-icon :last-page-icon]}]
  {:itemTypeToComponent
   {js/ReactUltimatePagination.ITEM_TYPES.PAGE
    (fn [props]
      (let [{:keys [value isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style button-style
            :label (str value)
            :secondary isActive
            :on-touch-tap onClick}])))

    js/ReactUltimatePagination.ITEM_TYPES.ELLIPSIS
    (fn [props]
      (r/as-element
        [ui/flat-button
         {:style button-style
          :label "..."
          :on-touch-tap (aget props "onClick")}]))

    js/ReactUltimatePagination.ITEM_TYPES.FIRST_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style button-style
            :icon first-page-icon
            :on-touch-tap onClick
            :disabled isActive}])))

    js/ReactUltimatePagination.ITEM_TYPES.PREVIOS_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style button-style
            :icon previous-page-icon
            :on-touch-tap onClick
            :disabled isActive}])))

    js/ReactUltimatePagination.ITEM_TYPES.NEXT_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style button-style
            :icon next-page-icon
            :on-touch-tap onClick
            :disabled isActive}])))

    js/ReactUltimatePagination.ITEM_TYPES.LAST_PAGE_LINK
    (fn [props]
      (let [{:keys [isActive onClick]} (js->clj props :keywordize-keys true)]
        (r/as-element
          [ui/flat-button
           {:style button-style
            :icon last-page-icon
            :on-touch-tap onClick
            :disabled isActive}])))}})

(defn create-pagination [config-opts]
  (r/adapt-react-class (js/ReactUltimatePagination.createUltimatePagination (clj->js (create-config config-opts)))))
