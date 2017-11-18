(ns name-bazaar.ui.pages.offering-create-page
  (:require
    [cljs-time.coerce :as time-coerce :refer [to-epoch to-date from-date]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [pos-ether-value?]]
    [district0x.ui.components.input :refer [input token-input]]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :refer [date+time->local-date-time format-time-duration-units]]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.date-picker :refer [date-picker]]
    [name-bazaar.ui.components.ens-record.ens-name-input :refer [ens-name-input]]
    [name-bazaar.ui.components.loading-placeholders :refer [content-placeholder]]
    [name-bazaar.ui.components.offering.offering-type-select :refer [offering-type-select]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [namehash sha3 normalize strip-eth-suffix valid-ens-name? path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn- label->full-name+node [label]
  (let [full-name (str label constants/registrar-root)]
    [full-name (namehash full-name)]))

(defn- get-ownership-status [value]
  (let [[full-name node] (label->full-name+node value)
        label-hash (sha3 value)
        loaded? (and @(subscribe [:ens.record/loaded? node])
                     (if (top-level-name? full-name)
                       @(subscribe [:registrar.entry.deed/loaded? label-hash])
                       true))]
    (cond
      (empty? value)
      :empty

      (not loaded?)
      :loading

      (not @(subscribe [:ens.record/active-address-owner? node]))
      :not-ens-record-owner

      (and (top-level-name? full-name)
           (not @(subscribe [:registrar.entry.deed/active-address-owner? label-hash])))
      :not-deed-owner

      :else :owner)))

(def ownership-status->text
  {:empty ""
   :loading "Checking ownership..."
   :not-ens-record-owner "You are not owner of this name"
   :not-deed-owner "You don't own this name's locked value"
   :owner "You are owner of this name"})

(defn offering-name-text-field [{:keys [:on-change :value :ownership-status] :as props}]
  (let [error? (contains? #{:not-ens-record-owner :not-deed-owner} ownership-status)]
    [:div.input-state-label
     {:class (cond
               error? :error
               (contains? #{:owner} ownership-status) :success)}
     [ens-name-input
      (r/merge-props
        {:label "Name"
         :fluid true
         :value value
         :error error?
         :on-change (fn [e data]
                      (let [value (aget data "value")]
                        (when (valid-ens-name? value)
                          (let [value (normalize value)
                                [full-name node] (label->full-name+node value)]
                            (aset data "value" value)
                            (on-change e data)
                            (dispatch [:ens.records/load [node]])
                            (when (top-level-name? full-name)
                              (dispatch [:registrar.entries/load [(sha3 value)]]))))))}
        (dissoc props :ownership-status :on-change))]
     [:div.ui.label (ownership-status->text ownership-status)]]))

(defn offering-default-form-data []
  (let [end-time (doto (to-date (t/plus (t/now) (t/days 3) (t/hours 1)))
                   (.setSeconds 0)
                   (.setMinutes 0))]
    {:offering/name ""
     :offering/type :auction-offering
     :offering/price 1
     :auction-offering/min-bid-increase 0.1
     :auction-offering/extension-duration 1
     :auction-offering/end-time end-time}))

(defn- hours->milis [hours]
  (* hours 3600000))

(defn- hours->seconds [hours]
  (* hours 3600))

(defn- seconds->hours [seconds]
  (/ seconds 3600))

(defn offering-extension-duration-slider [{:keys [:value] :as props}]
  (let [duration-formatted (format-time-duration-units (hours->milis value))]
    [:div
     [:h5.sub.heading "Time Extension"]
     [:input.extension-duration-slider
      (r/merge-props
        {:type "range"
         :min 0
         :max 24
         :step 0.25}
        props)]
     [:div.input-info
      (if (zero? (js/parseFloat value))
        "Auction will end exactly at end time with no possible extension."
        (str
          "If new highest bid arrives less than " duration-formatted " before auction end time, "
          "the auction will be extended by another " duration-formatted "."))]]))

(defn auction-end-time-date-picker [props]
  [:div.ui.input.labeled.dirty.fluid.auction-end-time
   [:div.ui.label "End Time"]
   [date-picker
    (r/merge-props
      {:show-time-select true
       :time-intervals 15
       :date-format "LLLL"
       :should-close-on-select false
       :max-date (.add (js/moment) (* 4 30) "days")
       :min-date (js/moment)}
      props)]])

(defn- form-data->transaction-data [{:keys [:offering/type] :as offering}]
  (-> offering
    (update :offering/name str constants/registrar-root)
    (update :auction-offering/end-time (comp from-date #(.toDate (js/moment %))))
    (update :auction-offering/extension-duration hours->seconds)))

(defn- transaction-data->form-data [{:keys [:offering/auction?] :as offering}]
  (cond-> offering
    true (update :offering/name strip-eth-suffix)
    auction? (update :auction-offering/end-time to-date)
    auction? (update :auction-offering/extension-duration seconds->hours)))

(defn offering-form [{:keys [:offering]}]
  (let [now (subscribe [:now])
        form-data (r/atom (or offering (offering-default-form-data)))]
    (fn [{:keys [:editing?]}]
      (let [{:keys [:offering/address :offering/name :offering/type :offering/price :auction-offering/min-bid-increase
                    :auction-offering/extension-duration :auction-offering/end-time]} @form-data
            auction? (= type :auction-offering)
            ownership-status (when-not editing? (get-ownership-status name))
            valid-price? (pos-ether-value? price)
            valid-min-bid-increase? (pos-ether-value? min-bid-increase)
            submit-disabled? (or (and (not editing?)
                                      (not= ownership-status :owner))
                                 (not valid-price?)
                                 (not (or (not auction?)
                                          valid-min-bid-increase?))
                                 (and auction?
                                      (t/after? @now (from-date (.toDate (js/moment end-time))))))]
        [ui/Grid
         {:class "layout-grid submit-footer offering-form"
          :celled "internally"}
         [ui/GridRow
          [ui/GridColumn
           {:width 16}
           [ui/Grid
            {:relaxed "very"}
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [offering-name-text-field
              {:value name
               :ownership-status ownership-status
               :disabled editing?
               :on-change #(swap! form-data assoc :offering/name (aget %2 "value"))}]]
            [ui/GridColumn
             {:computer 8
              :mobile 16
              :vertical-align :bottom}
             [offering-type-select
              {:value type
               :fluid true
               :disabled editing?
               :on-change #(swap! form-data assoc :offering/type (keyword (aget %2 "value")))}]]
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [token-input
              {:label (if auction? "Starting Price" "Price")
               :class "offering-price"
               :fluid true
               :value price
               :error (not valid-price?)
               :on-change #(swap! form-data assoc :offering/price (aget %2 "value"))}]]
            (when auction?
              [ui/GridColumn
               {:computer 8
                :mobile 16}
               [token-input
                {:label "Min. Bid Increase"
                 :class "offering-min-bid-increase"
                 :fluid true
                 :value min-bid-increase
                 :error (not valid-min-bid-increase?)
                 :on-change #(swap! form-data assoc :auction-offering/min-bid-increase (aget %2 "value"))}]])
            (when auction?
              [ui/GridColumn
               {:mobile 16
                :class :join-upper}
               [:div.input-info.min-bid-increase
                "New bids will need to be at least " min-bid-increase " ETH higher than previous highest bid."]])]]]
         (when auction?
           [ui/GridRow
            [ui/GridColumn
             {:width 16}
             [offering-extension-duration-slider
              {:value extension-duration
               :on-change #(swap! form-data assoc :auction-offering/extension-duration (aget % "target" "value"))}]]])
         (when (or auction? (not editing?))
           [ui/GridRow
            {:class (when (not auction?) :hide-divider)}
            (when auction?
              [ui/GridColumn
               {:computer 8
                :mobile 16}
               [auction-end-time-date-picker
                {:selected end-time
                 :on-change #(swap! form-data assoc :auction-offering/end-time %)}]])
            (when-not editing?
              [ui/GridColumn
               {:mobile 16
                :class "join-upper"}
               [:p.input-info
                (if auction?
                  "You will be able to edit parameters of this auction as long as there are no bids."
                  "You will be able to edit offering price even after creation.")]
               [:p.input-info
                [:b "IMPORTANT:"]
                " After creating an auction, you must transfer ownership of the name to the auction contract
                in order for it to display in search and for the name to be able to be sold. You will be notified once
                the name can be transferred, or you can complete this process from the "
                [:a {:href (path-for :route.user/my-offerings)} "My Offerings page"]
                " at a later time."]])])
         [ui/GridRow
          {:centered true}
          [:div
           (if-not editing?
             [ui/Button
              {:primary true
               :disabled submit-disabled?
               :on-click (fn []
                           (dispatch [(if auction?
                                        :auction-offering-factory/create-offering
                                        :buy-now-offering-factory/create-offering)
                                      (form-data->transaction-data @form-data)])
                           (when-not editing?
                             (swap! form-data assoc :offering/name "")))}
              "Create Offering"]
             [transaction-button
              {:primary true
               :disabled submit-disabled?
               :pending? (if auction?
                           @(subscribe [:auction-offering.set-settings/tx-pending? address])
                           @(subscribe [:buy-now-offering.set-settings/tx-pending? address]))
               :pending-text "Saving Changes..."
               :on-click (fn []
                           (dispatch [(if auction?
                                        :auction-offering/set-settings
                                        :buy-now-offering/set-settings)
                                      (form-data->transaction-data @form-data)]))}
              "Save Changes"])]]]))))

(defmethod page :route.offerings/edit []
  (let [route-params (subscribe [:district0x/route-params])]
    (fn []
      (let [{:keys [:offering/address]} @route-params
            offering-loaded? @(subscribe [:offering/loaded? address])
            offering @(subscribe [:offering address])]
        [app-layout
         {:meta {:title "NameBazaar - Edit Offering" :description "Edit an offering sell your ENS name"}}
         [ui/Segment
          [:h1.ui.header.padded "Edit Offering"]

          (cond
            (not offering-loaded?)
            [:div.padded [content-placeholder]]

            (not @(subscribe [:offering/active-address-original-owner? address]))
            [:div.padded "This offering wasn't created from your address, therefore you can't edit it."]

            (:offering/new-owner offering)
            [:div.padded "This offering was already bought by "
             [:a
              {:href (path-for :route.user/purchases (:offering/new-owner offering))}
              (:offering/new-owner offering)]]

            (and (:offering/auction? offering)
                 (pos? (:auction-offering/bid-count offering)))
            [:div.padded
             "This auction already has some bids, so it can't be edited anymore"]

            :else
            [offering-form
             {:editing? true
              :offering (transaction-data->form-data offering)}])]]))))

(defmethod page :route.offerings/create []
  [app-layout {:meta {:title "NameBazaar - Create Offering" :description "Create an offering to sell your ENS name"}}
   [ui/Segment
    [:h1.ui.header.padded "Create Offering"]
    [offering-form]]])
