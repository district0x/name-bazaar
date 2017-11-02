(ns name-bazaar.ui.components.offering.list-item
  (:require
    [cljs-time.core :as t]
    [clojure.string :as string]
    [district0x.shared.utils :refer [empty-address?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [format-eth-with-code format-local-datetime time-ago time-remaining-biggest-unit time-unit->text pluralize time-unit->short-text]]
    [name-bazaar.shared.utils :refer [emergency-state-new-owner]]
    [name-bazaar.ui.components.add-to-watched-names-button :refer [add-to-watched-names-button]]
    [name-bazaar.ui.components.ens-record.etherscan-link :refer [ens-record-etherscan-link]]
    [name-bazaar.ui.components.infinite-list :refer [expandable-list-item]]
    [name-bazaar.ui.components.loading-placeholders :refer [list-item-placeholder]]
    [name-bazaar.ui.components.offering.bottom-section :refer [offering-bottom-section]]
    [name-bazaar.ui.components.offering.general-info :refer [offering-general-info]]
    [name-bazaar.ui.components.offering.middle-section :refer [offering-middle-section]]
    [name-bazaar.ui.components.offering.tags :refer [offering-status-tag offering-sold-tag offering-auction-winning-tag offering-auction-pending-returns-tag]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [etherscan-ens-url path-for offering-type->text offering-type->icon]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn offering-detail-link [{:keys [:offering/address]}]
  [:div
   [:a.no-decor
    {:href (path-for :route.offerings/detail {:offering/address address})}
    "Open Offering Detail"]])

(defn links-section [{:keys [:offering]}]
  (let [{:keys [:offering/address :offering/name]} offering]
    [:div.description.links-section
     [offering-detail-link
      {:offering/address address}]
     [ens-record-etherscan-link
      {:ens.record/name name}]
     [add-to-watched-names-button
      {:ens.record/name name}]]))

(defn offering-expanded-body [{:keys [:offering]}]
  (let [{:keys [:offering/address :offering/name :offering/contains-non-ascii? :offering/top-level-name?
                :offering/original-owner]} offering]
    [ui/Grid
     {:class "layout-grid submit-footer offering-detail"
      :celled :internally}
     [ui/GridRow
      [ui/GridColumn
       {:computer 8
        :tablet 8
        :mobile 16}
       [offering-general-info
        {:offering offering}]]
      [ui/GridColumn
       {:computer 8
        :tablet 8
        :text-align :right
        :mobile 16}
       [links-section
        {:offering offering}]]]
     [ui/GridRow
      {:centered true}
      [offering-middle-section
       {:offering offering}]]
     [ui/GridRow
      {:centered true}
      [offering-bottom-section
       {:offering offering}]]]))

(defn offering-header-price [{:keys [:offering]}]
  [:div.offering-price
   (format-eth-with-code (:offering/price offering))])

(defn offering-header-active-tag [{:keys [:offering] :as props}]
  (when @(subscribe [:offering/active? (:offering/address offering)])
    [offering-status-tag
     {:offering/status :offering.status/active}]))

(defn offering-header-auction-winning-tag [{:keys [:offering] :as props}]
  (when @(subscribe [:auction-offering/active-address-winning-bidder? (:offering/address offering)])
    [offering-auction-winning-tag
     (r/merge-props
       {:won? (t/after? (t/now) (:auction-offering/end-time offering))}
       (dissoc props :offering))]))

(defn offering-header-auction-pending-returns-tag [{:keys [:offering] :as props}]
  (when (pos? @(subscribe [:auction-offering/active-address-pending-returns (:offering/address offering)]))
    [offering-auction-pending-returns-tag
     (dissoc props :offering)]))

(defn offering-header-missing-ownership-tag [{:keys [:offering] :as props}]
  (when @(subscribe [:offering/missing-ownership? (:offering/address offering)])
    [offering-status-tag
     {:offering/status :offering.status/missing-ownership}
     "Ownership"]))

(defn offering-header-sold-tag [{:keys [:offering] :as props}]
  (let [{:keys [:offering/new-owner]} offering]
    (when (and (not (empty? new-owner))
               (not= emergency-state-new-owner new-owner))
      [offering-sold-tag])))

(defn offering-header-offering-type [{:keys [:offering]}]
  (let [offering-type (:offering/type offering)]
    [:h6.ui.sub.header
     [:i.icon
      {:class (offering-type->icon offering-type)}]
     [:span.offering-type
      {:class offering-type}
      (offering-type->text offering-type)]]))

(defn offering-header-main-text [{:keys [:offering :show-created-on?]}]
  (let [{:keys [:offering/created-on :offering/name]} offering]
    [:div.offering-main-text
     (if show-created-on?
       (format-local-datetime created-on)
       name)]))


(defn offering-header-tags [{:keys [:show-sold? :show-active? :show-auction-winning?
                                    :show-auction-pending-returns? :show-missing-ownership? :offering] :as p}]  
  [:div.tags
   (when show-active?
     [offering-header-active-tag
      {:offering offering}])

   (when show-auction-winning?
     [offering-header-auction-winning-tag
      {:offering offering}])

   (when show-auction-pending-returns?
     [offering-header-auction-pending-returns-tag
      {:offering offering}])

   (when show-missing-ownership?
     [offering-header-missing-ownership-tag
      {:offering offering}])

   ;; TODO
   (when (and show-sold? (not (:offering/unregistered? offering)))
     [offering-header-sold-tag
      {:offering offering}])])

(defn offering-list-item-header-mobile []
  (let [visible? (r/atom false)]
    (fn [{:keys [:offering :show-created-on? :show-finalized-on?] :as props}]
      (let [{:keys [:offering/address :offering/auction? :offering/price
                    :offering/type :offering/finalized-on :offering/new-owner :auction-offering/bid-count
                    :auction-offering/end-time]} offering]
        [:div.search-results-list-item.mobile
         (if-not address
           [list-item-placeholder
            {:class "short"}]
           [:div.search-results-list-item-header
            {:class (when @visible? :opacity-1)
             :ref #(reset! visible? true)}                  ;; For fade-in animation
            [:div.left-section
             [offering-header-offering-type
              {:offering offering}]
             [offering-header-main-text
              {:show-created-on? show-created-on?
               :offering offering}]
             [offering-header-price
              {:offering offering}]]
            [:div.right-section
             {:class (when (and address
                                (or (not auction?)
                                    (and auction? end-time bid-count)))
                       :opacity-1)}

             [offering-header-tags props]

             (when auction?
               [:div.offering-auction-item
                [:div.amount bid-count]
                [:div.text "Bids"]])
             (when auction?
               (let [[unit amount] (if-not show-finalized-on?
                                     (time-remaining-biggest-unit (t/now) end-time)
                                     (time-remaining-biggest-unit finalized-on (t/now)))]
                 [:div.offering-auction-item.time-left
                  [:div.amount amount]
                  [:div.text (string/capitalize (str (pluralize (time-unit->text unit) amount)
                                                     (if-not show-finalized-on?
                                                       " left"
                                                       " ago")))]]))

             (when-not auction?
               [:div.offering-buy-now-item
                [ui/Button
                 "Buy Now"]])]])]))))

(defn offering-list-item-header []
  (let [visible? (r/atom false)]
    (fn [{:keys [:offering :show-created-on? :show-finalized-on?] :as props}]
      (let [{:keys [:offering/address :offering/auction? :offering/name :offering/price :offering/created-on
                    :offering/type :offering/finalized-on :offering/new-owner :auction-offering/bid-count
                    :auction-offering/end-time]} offering]
        [:div.ui.grid.padded.search-results-list-item
         (if-not address
           [list-item-placeholder]
           [ui/GridRow
            {:ref #(reset! visible? true)                   ;; For fade-in animation
             :class (str "search-results-list-item-header "
                         (when @visible? "opacity-1"))
             :vertical-align :middle}
            [ui/GridColumn
             {:width 4}
             [offering-header-main-text
              {:show-created-on? show-created-on?
               :offering offering}]]
            [ui/GridColumn
             {:width 2
              :text-align :center}
             [offering-header-offering-type
              {:offering offering}]]
            [ui/GridColumn
             {:width 3
              :text-align :center}
             [:div.offering-auction-item
              {:class (if (and auction? bid-count) "opacity-1" "opacity-0")}
              [:div.amount bid-count]]]
            [ui/GridColumn
             {:width 3
              :text-align :center}
             (let [[unit amount] (if-not show-finalized-on?
                                   (time-remaining-biggest-unit (t/now) end-time)
                                   (time-remaining-biggest-unit finalized-on (t/now)))]
               [:div.offering-auction-item.time-left
                {:class (if (or (and auction? end-time)
                                show-finalized-on?)
                          :opacity-1
                          :opacity-0)}
                [:div.amount amount " " [:span.unit (cond-> (time-unit->short-text unit)
                                                      (contains? #{:days :hours} unit) (pluralize amount))]]])]
            [ui/GridColumn
             {:class "price-column"
              :width 4
              :text-align :right
              :vertical-align :middle}
             [offering-header-tags props]
             [offering-header-price
              {:offering offering}]]])]))))

(defn offering-list-item []
  (let [mobile? (subscribe [:district0x.window.size/mobile?])]
    (fn [{:keys [:offering :expanded? :on-expand :key :header-props :body-props :disable-expand? :on-click]
          :as props}]
      (let [{:keys [:offering/address :offering/auction?]} offering
            mobile? (if (:mobile? props) true @mobile?)]
        [expandable-list-item
         {:index key
          :on-collapse #(dispatch [:offerings.list-item/collapsed offering])
          :on-expand #(dispatch [:offerings.list-item/expanded offering])
          :collapsed-height (constants/infinite-list-collapsed-item-height mobile?)
          :disable-expand? (or disable-expand? (not address))
          :on-click on-click}
         (if mobile?
           [offering-list-item-header-mobile
            (merge {:offering offering}
                   header-props)]
           [offering-list-item-header
            (merge {:offering offering}
                   header-props)])
         [offering-expanded-body
          (merge {:offering offering}
                 body-props)]]))))
