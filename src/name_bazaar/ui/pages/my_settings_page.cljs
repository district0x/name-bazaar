(ns name-bazaar.ui.pages.my-settings-page
  (:require
    [district0x.ui.components.misc :as misc :refer [row row-with-cols col paper page]]
    [district0x.ui.components.text-field :refer [text-field]]
    [district0x.ui.components.transaction-button :refer [raised-transaction-button]]
    [district0x.ui.utils :refer [valid-email?]]
    [medley.core :as medley]
    [name-bazaar.ui.components.misc :refer [a side-nav-menu-center-layout]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn email-notifications-form []
  (let [active-address (subscribe [:district0x/active-address])
        xs? (subscribe [:district0x/window-xs-width?])
        email-value (r/atom "")
        active-address-has-email? (subscribe [:district0x-emails/active-address-has-email?])]
    (fn []
      (let [valid? (valid-email? @email-value {:allow-empty? true})]
        [:div
         [:h2 "Email Notifications"]
         [text-field
          {:floating-label-text "Email"
           :value @email-value
           :full-width @xs?
           :error-text (when-not valid? " ")
           :on-change #(reset! email-value %2)}]
         (when @active-address-has-email?
           [:div
            {:style styles/margin-top-gutter-less}
            "Your address has encrypted email associated already"])
         [:div
          {:style styles/margin-top-gutter-less}
          "Email associated with your address will be encrypted and stored on a public blockchain."
          "Only our email server will be able to decrypt it. We'll use it to send you notifications about
           your purchases, sells and offering requests."]
         [row
          {:end "xs"
           :style styles/margin-top-gutter-more}
          [raised-transaction-button
           {:primary true
            :label "Save"
            :full-width @xs?
            :pending? @(subscribe [:district0x-emails.set-email/tx-pending? @active-address])
            :pending-label "Saving..."
            :disabled (or (empty? @email-value)
                          (not valid?))
            :on-click (fn []
                        (dispatch [:district0x-emails/set-email
                                   {:district0x-emails/email @email-value}
                                   {:on-tx-receipt [:district0x.snackbar/show-message "Your email was successfully updated"]}]))}]]]))))

(defmethod page :route.user/my-settings []
  [side-nav-menu-center-layout
   [paper
    [:h1
     {:style (merge styles/page-headline styles/margin-bottom-gutter)}
     "My Settings"]
    [email-notifications-form]
    ]])
