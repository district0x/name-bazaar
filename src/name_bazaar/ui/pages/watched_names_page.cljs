(ns name-bazaar.ui.pages.watched-names-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [district0x.ui.components.text-field :refer [text-field-with-suffix]]
    [district0x.ui.utils :refer [current-component-mui-theme]]
    [medley.core :as medley]
    [name-bazaar.ui.components.ens-name-details :refer [ens-name-details]]
    [name-bazaar.ui.components.icons :as icons]
    [name-bazaar.ui.components.infinite-list :refer [infinite-list expandable-list-item]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.components.offering-request.list-item :refer [offering-request-list-item]]
    [name-bazaar.ui.components.offering.list-item :refer [offering-list-item]]
    [name-bazaar.ui.components.search-results.infinite-list :refer [search-results-infinite-list]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [valid-ens-name?]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn- add-to-watched-names! [new-name-atom]
  (when (seq @new-name-atom)
    (dispatch [:watched-names/add (str @new-name-atom constants/registrar-root)])
    (reset! new-name-atom "")))

(defn add-watched-name-form []
  (let [xs? (subscribe [:district0x/window-xs-width?])
        new-name (r/atom "")]
    (fn []
      [:div
       {:style styles/margin-left-gutter-less}
       [text-field-with-suffix
        {:floating-label-text "Enter Name"
         :full-width @xs?
         :value @new-name
         :on-key-press (fn [e]
                         (when (= (aget e "key") "Enter")
                           (add-to-watched-names! new-name)))
         :on-change (fn [e value]
                      (when (valid-ens-name? value)
                        (reset! new-name value)))}
        [row
         [:span
          {:style styles/text-field-suffix}
          constants/registrar-root]
         [ui/icon-button
          {:style styles/add-to-watched-names-button
           :on-click #(add-to-watched-names! new-name)}
          (icons/plus
            {:color (current-component-mui-theme "paper" "color")})]]]])))

(defn watch-item-placeholder []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:watched-name] :as props}]
      (let [{:keys [:ens.record/name]} watched-name]
        [expandable-list-item
         {:expand-disabled? true
          :collapsed-height (styles/search-results-list-item-height @xs?)}
         [:div
          (r/merge-props
            {:style (styles/search-results-list-item @xs?)}
            (dissoc props :watched-name))
          [row-with-cols
           {:style (merge styles/search-results-list-item-header)
            :between "sm"
            :middle "sm"}
           [col
            {:xs 12 :sm 5}
            [:div
             {:style styles/list-item-ens-record-name}
             name]]]]]))))

(defn remove-all-button []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn []
      [row
       {:end "sm"}
       [:a
        {:style (merge styles/margin-right-gutter-less
                       styles/margin-left-gutter-less
                       (when @xs?
                         styles/margin-top-gutter-less))
         :on-click #(dispatch [:watched-names/remove-all])}
        "Clear All"]])))

(defn watched-names-infinite-list []
  (let [xs? (subscribe [:district0x/window-xs-width?])
        watched-items (subscribe [:watched-names/watched-items])]
    (fn []
      [:div
       {:style styles/margin-top-gutter-less}
       [infinite-list
        {:collapsed-item-height (styles/search-results-list-item-height @xs?)
         :total-count (count @watched-items)
         :no-items-element (r/as-element [:div
                                          {:style styles/search-results-no-items}
                                          "You are not watching any names"])}
        (doall
          (for [{:keys [:ens.record/node] :as watched-item} @watched-items]
            (cond
              (:offering/address watched-item)
              [offering-list-item
               {:key node
                :offering watched-item}]

              (:offering-request/requesters-count watched-item)
              [offering-request-list-item
               {:key node
                :offering-request watched-item}]

              :else
              [watch-item-placeholder
               {:key node
                :watched-name watched-item}])))]])))

(defmethod page :route/watched-names []
  [side-nav-menu-center-layout
   [paper
    {:style styles/search-results-paper}
    [:h1
     {:style styles/search-results-paper-headline}
     "Watched Names"]
    [add-watched-name-form]
    [remove-all-button]
    [watched-names-infinite-list]]])
