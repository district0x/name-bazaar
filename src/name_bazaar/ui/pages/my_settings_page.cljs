(ns name-bazaar.ui.pages.my-settings-page
  (:require
    [district0x.ui.components.input :refer [input]]
    [district0x.ui.components.misc :as misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :refer [valid-email?]]
    [medley.core :as medley]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn email-notifications-form []
  (let [active-address (subscribe [:district0x/active-address])
        email-value (r/atom "")
        active-address-has-email? (subscribe [:district0x-emails/active-address-has-email?])]
    (fn []
      (let [valid? (valid-email? @email-value {:allow-empty? true})]
        [ui/Grid
         {:class "layout-grid submit-footer"}
         [ui/GridRow
          [ui/GridColumn
           {:mobile 16
            :tablet 8
            :computer 6}
           [input
            {:label "Email"
             :fluid true
             :value @email-value
             :error (not valid?)
             :on-change #(reset! email-value (aget %2 "value"))}]]]
         [ui/GridRow
          [ui/GridColumn
           {:class :join-upper}
           (when @active-address-has-email?
             [:div.description.success "Your address has encrypted email associated already"])
           [:div.description
            "Email associated with your address will be encrypted and stored on a public blockchain. "
            "Only our email server will be able to decrypt it. We'll use it to send you notifications about
             your purchases, sells and offering requests."]]]
         [ui/GridRow
          {:centered true}
          [:div
           [transaction-button
            {:primary true
             :disabled (or (empty? @email-value)
                           (not valid?))
             :pending? @(subscribe [:district0x-emails.set-email/tx-pending? @active-address])
             :pending-text "Saving..."
             :on-click (fn []
                         (dispatch [:district0x-emails/set-email
                                    {:district0x-emails/email @email-value}
                                    {:on-tx-receipt [:district0x.snackbar/show-message "Your email was successfully updated"]}]))}
            "Save"]]]]))))

(defmethod page :route.user/my-settings []
  [app-layout {:meta {:title "NameBazaar - My Settings" :description "Configure settings associated with your Ethereum address."}}
   [ui/Segment
    [:h1.ui.header.padded "My Settings"]
    [email-notifications-form]]])
