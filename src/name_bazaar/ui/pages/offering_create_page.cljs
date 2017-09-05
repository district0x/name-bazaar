(ns name-bazaar.ui.pages.offering-create-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljs-time.coerce :as time-coerce :refer [to-epoch to-date]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [pos-ether-value?]]
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [district0x.ui.components.text-field :refer [text-field-with-suffix ether-field-with-currency]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [district0x.ui.utils :as d0x-ui-utils :refer [current-component-mui-theme date+time->local-date-time]]
    [name-bazaar.shared.utils :refer [name-level]]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.components.search-results.list-item-placeholder :refer [list-item-placeholder]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.styles :as styles]
    [name-bazaar.ui.utils :refer [namehash sha3 strip-eth-suffix]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn- label->full-name+node [label]
  (let [full-name (str label constants/registrar-root)]
    [full-name (namehash full-name)]))

(defn- get-ownership-status [value]
  (let [[full-name node] (label->full-name+node value)
        label-hash (sha3 value)
        loaded? (and @(subscribe [:ens.record/loaded? node])
                     (if (= 1 (name-level full-name))
                       @(subscribe [:registrar.entry.deed/loaded? label-hash])
                       true))]
    (cond
      (empty? value)
      :empty

      (not loaded?)
      :loading

      (not @(subscribe [:ens.record/active-address-owner? node]))
      :not-ens-record-owner

      (not @(subscribe [:registrar.entry.deed/active-address-owner? label-hash]))
      :not-deed-owner

      :else :owner)))

(def ownership-status->text
  {:empty ""
   :loading "Checking ownership..."
   :not-ens-record-owner "You are not owner of this name"
   :not-deed-owner "You don't own this name's locked value"
   :owner "You are owner of this name"})

(def ownership-status-color
  {:loading styles/text-field-warning-color
   :owner styles/text-field-success-color})

(defn offering-name-text-field []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:on-change :value :ownership-status] :as props}]
      (let [error-color (ownership-status-color ownership-status)]
        [text-field-with-suffix
         (r/merge-props
           {:floating-label-text "Name"
            :full-width @xs?
            :error-text (ownership-status->text ownership-status)}
           (merge
             (when error-color
               {:error-style {:color error-color}
                :floating-label-style {:color error-color}})
             (dissoc props :ownership-status)
             {:on-change (fn [e value]
                           (let [[full-name node] (label->full-name+node value)]
                             (on-change e value)
                             (dispatch [:ens.records/load [node]])
                             (when (= 1 (name-level full-name))
                               (dispatch [:registrar.entry/load (sha3 value)]))))}))
         [:span
          {:style styles/text-field-suffix}
          constants/registrar-root]]))))

(defn offering-type-select-field []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [props]
      [ui/select-field
       (r/merge-props
         {:hint-text "Offering Type"}
         props)
       (doall
         (for [[val text] [[:buy-now-offering "Buy Now"] [:auction-offering "Auction"]]]
           [ui/menu-item
            {:key (name val)
             :value (name val)
             :primary-text text}]))])))

(defn offering-default-form-data []
  (let [end-time (time-coerce/to-date (t/plus (t/now) (t/days 3)))]
    {:offering/name ""
     :offering/type :auction-offering
     :offering/price 1
     :auction-offering/min-bid-increase 0.1
     :auction-offering/extension-duration 1
     :auction-offering.end-time/date end-time
     :auction-offering.end-time/time end-time}))

(defn- hours->milis [hours]
  (* hours 3600000))

(defn- hours->seconds [hours]
  (* hours 3600))

(defn- seconds->hours [seconds]
  (/ seconds 3600))

(defn offering-min-bid-increase-text-field []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:value] :as props}]
      [:div
       [ether-field-with-currency
        (r/merge-props
          {:only-positive? true
           :full-width @xs?}
          props)]
       [:div
        {:style styles/margin-top-gutter-less}
        "New bids will need to be at least " value " ETH higher than previous highest bid."]])))

(defn offering-extension-duration-slider []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:value] :as props}]
      (let [duration-formatted (d0x-ui-utils/format-time-duration-units (hours->milis value))]
        [:div
         {:style styles/margin-top-gutter}
         [:div
          {:style {:color (current-component-mui-theme "textField" "floatingLabelColor")}}
          "Time Extension"]
         [ui/slider
          (r/merge-props
            {:style {:width (if @xs? "100%" 256)}
             :slider-style {:margin-bottom styles/desktop-gutter-mini}
             :min 0
             :max 8
             :step 0.25}
            props)]
         [:div
          {:style (merge {:width (if @xs? "100%" 256)}
                         styles/text-center)}
          (if (zero? value)
            "no extension"
            ) duration-formatted]
         (if (zero? value)
           [:div
            {:style styles/margin-top-gutter-less}
            "Auction will end exactly at end time with no possible extension."]
           [:div
            {:style styles/margin-top-gutter-less}
            "If new highest bid arrives less than " duration-formatted " before auction end time, "
            "the auction will be extended by another " duration-formatted "."])]))))

(defn auction-end-time-date-picker []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:value] :as props}]
      [ui/date-picker
       (r/merge-props
         {:min-date (time-coerce/to-date (t/now))
          :floating-label-text "End Date"
          :text-field-style (if @xs? {:width "100%"} {})
          :format-date (fn [x]
                         (.toDateString x))}
         props)])))

(defn auction-end-time-time-picker []
  (let [xs? (subscribe [:district0x/window-xs-width?])]
    (fn [{:keys [:value] :as props}]
      [ui/time-picker
       (r/merge-props
         {:floating-label-text "End Time"
          :text-field-style (if @xs? {:width "100%"} {})
          ;:minutes-step 10
          }
         props)])))

(defn- form-data->transaction-data [offering]
  (let [auction? (= (:offering/type offering) :auction-offering)]
    (cond-> offering
      true (update :offering/name str constants/registrar-root)
      auction? (assoc :auction-offering/end-time
                      (date+time->local-date-time (:auction-offering.end-time/date offering)
                                                  (:auction-offering.end-time/time offering)))
      auction? (update :auction-offering/extension-duration
                       hours->seconds))))

(defn- transaction-data->form-data [offering]
  (let [auction? (= (:offering/type offering) :auction-offering)]
    (cond-> offering
      true (update :offering/name strip-eth-suffix)
      auction? (assoc :auction-offering.end-time/date (to-date (:auction-offering/end-time offering)))
      auction? (assoc :auction-offering.end-time/time (to-date (:auction-offering/end-time offering)))
      auction? (update :auction-offering/extension-duration seconds->hours))))

(defn- get-submit-event [offering-type editing?]
  (if (= offering-type :buy-now-offering)
    (if editing?
      :buy-now-offering/set-settings
      :buy-now-offering-factory/create-offering)
    (if editing?
      :auction-offering/set-settings
      :auction-offering-factory/create-offering)))


(defn offering-form [{:keys [:offering]}]
  (let [xs? (subscribe [:district0x/window-xs-width?])
        form-data (r/atom (or offering (offering-default-form-data)))]
    (fn [{:keys [:editing?]}]
      (let [{:keys [:offering/name :offering/type :offering/price :auction-offering/min-bid-increase
                    :auction-offering/extension-duration :auction-offering.end-time/date
                    :auction-offering.end-time/time]} @form-data
            auction? (= type :auction-offering)
            ownership-status (when-not editing? (get-ownership-status name))]
        [:div
         [:div
          {:style {:height 80}}
          [offering-name-text-field
           {:value name
            :ownership-status ownership-status
            :disabled editing?
            :on-change #(swap! form-data assoc :offering/name %2)}]]
         [offering-type-select-field
          {:value type
           :disabled editing?
           :on-change #(swap! form-data assoc :offering/type (keyword %3))}]
         [ether-field-with-currency
          {:full-width @xs?
           :value price
           :only-positive? true
           :floating-label-text (if auction? "Start Price" "Price")
           :on-change #(swap! form-data assoc :offering/price %2)}]
         (when auction?
           [:div
            [offering-min-bid-increase-text-field
             {:floating-label-text (if auction? "Min. Bid Increase")
              :value min-bid-increase
              :on-change #(swap! form-data assoc :auction-offering/min-bid-increase %2)}]
            [offering-extension-duration-slider
             {:value extension-duration
              :on-change #(swap! form-data assoc :auction-offering/extension-duration %2)}]
            [auction-end-time-date-picker
             {:value date
              :on-change #(swap! form-data assoc :auction-offering.end-time/date %2)}]
            [auction-end-time-time-picker
             {:value time
              :on-change #(swap! form-data assoc :auction-offering.end-time/time %2)}]])
         (when-not editing?
           [:div
            {:style styles/margin-top-gutter-less}
            (if auction?
              "You will be able to edit parameters of this auction as long as there are no bids."
              "You will be able to edit offering price even after creation.")])
         (when-not editing?
           [:div
            {:style styles/margin-top-gutter-less}
            "IMPORTANT: After you create offering contract, you will need to transfer name ownership into it "
            "in order to display it in search and for others being able to buy it. You will be notified once the contract "
            "is ready, or you can do it from " [a {:route :route.user/my-offerings} "My Offerings"] " page later."])
         [row
          {:end "xs"
           :style styles/margin-top-gutter-more}
          [raised-transaction-button
           {:primary true
            :label (if editing? "Save Changes" "Create Offering")
            :full-width @xs?
            :disabled (or (and (not editing?)
                               (not= ownership-status :owner))
                          (not (pos-ether-value? price))
                          (not (or (not auction?)
                                   (pos-ether-value? min-bid-increase))))
            :on-click (fn []
                        (dispatch [(get-submit-event type editing?) (form-data->transaction-data @form-data)])
                        (when-not editing?
                          (swap! form-data assoc :offering/name "")))}]]]))))

(defn loading-placeholder []
  [:div
   [list-item-placeholder
    {:style styles/margin-top-gutter}]
   [list-item-placeholder
    {:style styles/margin-top-gutter}]
   [list-item-placeholder
    {:style styles/margin-top-gutter}]])

(defn no-permission-error [text]
  [row
   {:style {:height 150}
    :middle "xs"
    :center "xs"}
   text])

(defmethod page :route.offerings/edit []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      (let [{:keys [:offering/address]} @route-params
            offering-loaded? @(subscribe [:offering/loaded? address])
            offering @(subscribe [:offering-registry/offering address])]
        [side-nav-menu-center-layout
         [paper
          [:h1 "Edit Offering"]

          (cond
            (not offering-loaded?)
            [loading-placeholder]

            (not @(subscribe [:offering/active-address-original-owner? address]))
            [no-permission-error
             "This offering wasn't created from your address, therefore you can't edit it."]

            (:offering/new-owner offering)
            [no-permission-error
             "This offering was already bought by " [a {:route :route.user/purchases
                                                        :route-params (:offering/new-owner offering)}
                                                     (:offering/new-owner offering)]]

            (and (= (:offering/type offering) :auction-offering)
                 (pos? (:auction-offering/bid-count offering)))
            [no-permission-error
             "This auction already has some bids, so it can't be edited anymore"]

            :else
            [offering-form
             {:editing? true
              :offering (transaction-data->form-data offering)}])]]))))

(defmethod page :route.offerings/create []
  [side-nav-menu-center-layout
   [paper
    [:h1 "Create Offering"]
    [offering-form]]])
