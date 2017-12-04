(ns name-bazaar.ui.pages.manage-names-page
  (:require
    [cljs-time.coerce :as time-coerce :refer [to-epoch to-date from-date]]
    [cljs-time.core :as t]
    [district0x.shared.utils :refer [pos-ether-value? empty-address?]]
    [district0x.ui.components.input :refer [input token-input]]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :refer [date+time->local-date-time format-time-duration-units]]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.date-picker :refer [date-picker]]
    [name-bazaar.ui.components.ens-record.ens-name-input :refer [ens-name-input-ownership-validated]]
    [name-bazaar.ui.components.loading-placeholders :refer [content-placeholder]]
    [name-bazaar.ui.components.offering.offering-type-select :refer [offering-type-select]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [reverse-record-node namehash sha3 normalize strip-root-registrar-suffix valid-ens-name? path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-web3.core :as web3]
    [soda-ash.core :as ui]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))



(defn default-point-name-form-data [{:keys [:address :name]}]
  {:ens.record/addr (or address "0x")
   :ens.record/name (or name "")})



(defn load-resolver [address]
  (let [node (reverse-record-node address)]
    (dispatch [:ens.records.resolver/load [node]])))

#_(defn ens-name-text-field [{:keys [:value]}]
  (fn [{:keys [:on-change :value] :as props}]
    (let [full-name (when (seq value)
                      (str value constants/registrar-root))
          ownership-status @(subscribe [:ens.record/ownership-status full-name])
          standard-resolver? @(subscribe [:ens.record/standard-resolver? (namehash full-name)])
          error? (contains? #{:ens.ownership-status/not-ens-record-owner
                              :ens.ownership-status/not-deed-owner}
                            ownership-status)]
      [:div.input-state-label
       {:class (cond
                 error? :error
                 (contains? #{:ens.ownership-status/owner} ownership-status) :success)}
       [ens-name-input
        (r/merge-props
          {:label "Name"
           :fluid true
           :value value
           :error error?
           :on-change (fn [e data]
                        (let [value (aget data "value")]
                          (when (valid-ens-name? value)
                            (let [value (normalize value)]
                              (aset data "value" value)
                              (on-change e data)
                              (load-name-ownership value)))))}
          (dissoc props :ownership-status :on-change))]
       [:div.ui.label (ownership-status->text ownership-status)]])))

(defn address-text-field [{:keys [:value :load-resolver?]}]
  (fn [{:keys [:on-change :value] :as props}]
    (let [is-empty? (or
                     (empty? value)
                     (empty-address? value))
          error? (and (not is-empty?)
                      (not (web3/address? value)))]
      [:div.input-state-label
       {:class (if error? :error
                   :success)}
       [input
        (r/merge-props
         {:label "Address"
          :fluid true
          :value value
          :error error?
          :on-change (fn [e data]
                       (let [value (aget data "value")]
                         (when (valid-ens-name? value)
                           (aset data "value" value)
                           (on-change e data)
                           (load-resolver value))))}
         (dissoc props :on-change))]
       [:div.ui.label (if error? "Isn't valid address"
                          (when-not is-empty? "Valid address"))]])))

(defn point-name-form [defaults]
  (let [form-data (r/atom (default-point-name-form-data defaults))]
    (fn [{:keys [:editing? :default-name]}]
      (let [{:keys [:ens.record/addr :ens.record/name]} @form-data
            ownership-status (when-not editing?
                               @(subscribe [:ens.record/ownership-status (when (seq name)
                                                                           (str name constants/registrar-root))]))
            full-name (when (seq name)
                        (str name constants/registrar-root))

            standard-resolver? @(subscribe [:ens.record/standard-resolver? (namehash full-name)])
            submit-disabled? (or (and (not editing?)
                                      (not= ownership-status :ens.ownership-status/owner)))]
        [ui/Grid
         {:class "layout-grid submit-footer offering-form"
          ;;:celled "internally"
          }
         [ui/GridRow
          [ui/GridColumn
           {:width 16}
           [ui/Grid
            {:relaxed "very"}
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [ens-name-text-field-ownership-validated
              {:value name
               :disabled editing?
               :on-change #(swap! form-data assoc :ens.record/name (aget %2 "value"))}]]
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [address-text-field
              {:value addr
               :disabled (or (not standard-resolver?)
                             editing?)
               :on-change #(swap! form-data assoc :ens.record/addr (aget %2 "value"))}]]]]]
         [ui/GridRow
          [ui/GridColumn
           {:mobile 16
            :class "join-upper"}
           [:p.input-info
            (str
             " Pointing  your name to address will allow other to send funds to " full-name ", instead of hexadecimal number.")]
           (when-not standard-resolver?
             [:p.input-info
              " Before you can point your name to an address, you must setup resolver for your address."])]]
         [ui/GridRow
          {:centered true}
          [:div
           (if-not standard-resolver?
             [transaction-button
              {:primary true
               :disabled submit-disabled?
               :pending? @(subscribe [:ens.set-resolver/tx-pending? (namehash full-name)])
               :pending-text "Setting resolver..."
               :on-click (fn []
                           (dispatch [:ens/set-resolver @form-data]))}
              "Setup resolver"]
             [transaction-button
              {:primary true
               :disabled submit-disabled?
               :pending? @(subscribe [:public-resolver.set-addr/tx-pending? (namehash full-name)])
               :pending-text "Pointing name..."
               :on-click (fn []
                           (dispatch [:public-resolver/set-addr @form-data]))}
              "Point name"])]]]))))

(defn point-address-form [defaults]
  (let [form-data (r/atom (default-point-name-form-data defaults))]
    (fn [{:keys [:editing?]}]
      (let [{:keys [:ens.record/addr :ens.record/name]} @form-data
            ownership-status (when-not editing?
                               @(subscribe [:ens.record/ownership-status (when (seq name)
                                                                           (str name constants/registrar-root))]))
            full-name (when (seq name)
                        (str name constants/registrar-root))

            standard-resolver? @(subscribe [:ens.record/standard-resolver? (reverse-record-node addr)])
            submit-disabled? (or (and (not editing?)
                                      (not= ownership-status :ens.ownership-status/owner)))]
        [ui/Grid
         {:class "layout-grid submit-footer offering-form"
          ;;:celled "internally"
          }
         [ui/GridRow
          [ui/GridColumn
           {:width 16}
           [ui/Grid
            {:relaxed "very"}
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [ens-name-text-field-ownership-validated
              {:value name
               :disabled (or (not standard-resolver?)
                             editing?)
               :on-change #(swap! form-data assoc :ens.record/name (aget %2 "value"))}]]
            [ui/GridColumn
             {:computer 8
              :mobile 16}
             [address-text-field
              {:value addr
               :disabled true;;editing?
               :on-change #(swap! form-data assoc :ens.record/addr (aget %2 "value"))}]]]]]
         [ui/GridRow
          [ui/GridColumn
           {:mobile 16
            :class "join-upper"}
           [:p.input-info
            (str
             "Your address " addr " will be pointing to name " full-name ". This will help Ethereum applications to figure out your username just from your address.")]
           (when-not standard-resolver?
             [:p.input-info
              " Before you can point your address to a name, you must setup resolver for your address."])]]
         [ui/GridRow
          {:centered true}
          [:div
           (if-not standard-resolver?
             [transaction-button
              {:primary true
               ;;:disabled submit-disabled?
               :pending? @(subscribe [:reverse-registrar.claim-with-resolver/tx-pending? addr])
               :pending-text "Setting resolver..."
               :on-click (fn []
                           (dispatch [:reverse-registrar/claim-with-resolver @form-data]))}
              "Setup resolver"]
             [transaction-button
              {:primary true
               :disabled submit-disabled?
               :pending? @(subscribe [:public-resolver.set-name/tx-pending? (reverse-record-node addr)])
               :pending-text "Pointing address..."
               :on-click (fn []
                           (dispatch [:public-resolver/set-name @form-data]))}
              "Point address"])]]]))))

(defmethod page :route.user/manage-names []
  (let [query-params (subscribe [:district0x/query-params])]
    (fn []
      (let [{:keys [:name]} @(subscribe [:district0x/query-params])
            address @(subscribe [:district0x/active-address])]
        [app-layout {:meta {:title "NameBazaar - Manage Names" :description "Manage your ENS names"}}
         [ui/Segment
          [:h1.ui.header.padded "Point Name to an Address"]
          [point-name-form
           {:name (when (and name (valid-ens-name? name))
                            (strip-root-registrar-suffix (normalize name)))
            :address address}]]
         [ui/Segment
          [:h1.ui.header.padded "Point Address to a Name"]
          [point-address-form
           {:name (when (and name (valid-ens-name? name))
                            (strip-root-registrar-suffix (normalize name)))
            :address address}]]]))))
