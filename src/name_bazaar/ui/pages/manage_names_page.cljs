(ns name-bazaar.ui.pages.manage-names-page
  (:require
    [district0x.shared.utils :refer [empty-address?]]
    [district0x.ui.components.input :refer [input]]
    [district0x.ui.components.misc :refer [page]]
    [district0x.ui.components.transaction-button :refer [transaction-button]]
    [district0x.ui.utils :refer [format-eth-with-code truncate]]
    [name-bazaar.shared.utils :refer [top-level-name? name-label]]
    [name-bazaar.ui.components.app-layout :refer [app-layout]]
    [name-bazaar.ui.components.ens-record.ens-name-input :refer [ens-name-input-ownership-validated]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [reverse-record-node namehash sha3 normalize strip-root-registrar-suffix valid-ens-name? valid-ens-subname? path-for]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-web3.core :as web3]
    [soda-ash.core :as ui]))


(defn default-point-name-form-data [{:keys [:address :name]}]
  {:ens.record/addr (or address "0x")
   :ens.record/name (or name "")})


(defn default-transfer-name-form-data [{:keys [:name]}]
  {:ens.record/owner ""
   :ens.record/name (or name "")})

(defn load-resolver [address]
  (let [node (reverse-record-node address)]
    (dispatch [:ens.records.resolver/load [node]])))

(defn load-addr [n]
  (let [node (namehash n)]
    (dispatch [:public-resolver.addr/load node])))

(defn address-text-field []
  (fn [{:keys [:on-change :value] :as props}]
    (let [is-empty? (empty-address? value)
          error? (and (not is-empty?)
                      (not (web3/address? value)))]
      [:div.input-state-label
       {:class (if error? :error :success)}
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
       [:div.ui.label (when error? "Invalid address")]])))

(defn subname-text-field []
  (fn [{:keys [:on-change :value] :as props}]
    [:div.input-state-label
     [input
      (r/merge-props
        {:label "Subname"
         :fluid true
         :value value
         :on-change (fn [e data]
                      (let [value (aget data "value")]
                        (when (valid-ens-subname? value)
                          (aset data "value" value)
                          (on-change e data)
                          (load-resolver value))))}
        (dissoc props :on-change))]]))

(defn point-name-form [defaults]
  (let [form-data (r/atom (default-point-name-form-data defaults))]
    (fn []
      (let [{:keys [:ens.record/addr :ens.record/name]} @form-data
            ownership-status @(subscribe [:ens.record/ownership-status (when (seq name)
                                                                         (str name constants/registrar-root))])
            full-name-fn #(when (seq %)
                            (str % constants/registrar-root))
            full-name (full-name-fn name)
            name-record @(subscribe [:public-resolver/record (namehash full-name)])
            default-resolver? @(subscribe [:ens.record/default-resolver? (namehash full-name)])
            submit-disabled? (not= ownership-status :ens.ownership-status/owner)]
        [:div.grid.submit-footer.offering-form
         [:div.name-ownership
          [ens-name-input-ownership-validated
           {:value name
            :on-change (fn [_ data]
                         (swap! form-data assoc :ens.record/name (aget data "value"))
                         (load-addr (full-name-fn (aget data "value"))))}]]
         [:div.address
          [address-text-field
           {:value addr
            :disabled (not default-resolver?)
            :on-change #(swap! form-data assoc :ens.record/addr (aget %2 "value"))}]]
         [:div.info
          [:p.input-info
           "Pointing  your name to an address enables others to send funds to "
           (or full-name "human-readable address")
           ", instead of hexadecimal number."]
          (when name-record
            [:p.input-info
             full-name " is currently pointed to " (:public-resolver.record/addr name-record) "."])
          (when-not default-resolver?
            [:p.input-info
             "Before you can point name to an address, you must setup resolver for your name."])]
         [:div.button
          (if-not default-resolver?
            [transaction-button
             {:primary true
              :disabled submit-disabled?
              :pending? @(subscribe [:ens.set-resolver/tx-pending? (namehash full-name)])
              :pending-text "Setting up resolver..."
              :on-click #(dispatch [:ens/set-resolver @form-data])}
             "Setup resolver"]
            [transaction-button
             {:primary true
              :disabled submit-disabled?
              :pending? @(subscribe [:public-resolver.set-addr/tx-pending? (namehash full-name)])
              :pending-text "Pointing name..."
              :on-click #(dispatch [:public-resolver/set-addr @form-data])}
             "Point name"])]]))))

(defn point-address-form [defaults]
  (let [form-data (r/atom (default-point-name-form-data defaults))]
    (fn []
      (let [{:keys [:ens.record/addr :ens.record/name]} @form-data
            full-name (when (seq name)
                        (str name constants/registrar-root))
            submit-disabled? (empty? name)
            addr-record @(subscribe [:public-resolver/reverse-record addr])
            default-resolver? @(subscribe [:ens.record/default-resolver? (reverse-record-node addr)])]
        [:div.grid.submit-footer.offering-form
         [:div.name-ownership [ens-name-input-ownership-validated
                               {:value name
                                :warn-only? true
                                :disabled (not default-resolver?)
                                :on-change #(swap! form-data assoc :ens.record/name (aget %2 "value"))}]]
         [:div.address
          [address-text-field
           {:value addr
            :disabled (not default-resolver?)
            :on-change #(swap! form-data assoc :ens.record/addr (aget %2 "value"))}]]
         [:div.info
          [:p.input-info
           "Your address " (truncate addr 10) " will be pointing to "
           (or full-name "chosen name")
           ". This will help Ethereum applications to find human-readable name from your address."]
          (when-not default-resolver?
            [:p.input-info
             "Before you can point address to a name, you must setup resolver for your address."])
          (when addr-record
            [:p.input-info
             addr " is currently pointed to " (:public-resolver.record/name addr-record) "."])]
         [:div.button
          (if-not default-resolver?
            [transaction-button
             {:primary true
              :pending? @(subscribe [:reverse-registrar.claim-with-resolver/tx-pending? addr])
              :pending-text "Setting up resolver..."
              :on-click #(dispatch [:reverse-registrar/claim-with-resolver @form-data])}
             "Setup resolver"]
            [transaction-button
             {:primary true
              :disabled submit-disabled?
              :pending? @(subscribe [:public-resolver.set-name/tx-pending? (reverse-record-node addr)])
              :pending-text "Pointing address..."
              :on-click #(dispatch [:public-resolver/set-name @form-data])}
             "Point address"])]]))))

(defn create-subname-form [defaults]
  (let [form-data (r/atom (default-point-name-form-data defaults))]
    (fn []
      (let [{:keys [:ens.record/subname :ens.record/name]} @form-data
            ownership-status @(subscribe [:ens.record/ownership-status
                                          (when (seq name)
                                            (str name constants/registrar-root))])
            full-name (when (seq name)
                        (str name constants/registrar-root))

            full-subname (str subname "." full-name)
            correct-subname? (and (not (empty? subname))
                                  (valid-ens-subname? subname)
                                  (valid-ens-name? full-subname))
            submit-disabled? (or
                               (not correct-subname?)
                               (not= ownership-status :ens.ownership-status/owner))]
        [:div.grid.submit-footer.offering-form
         [:div.name-ownership
          [ens-name-input-ownership-validated
           {:value name
            :on-change #(swap! form-data assoc :ens.record/name (aget %2 "value"))}]]

         [:div.subname
          [subname-text-field
           {:value subname
            :on-change #(swap! form-data assoc :ens.record/subname (aget %2 "value"))}]]
         [:div.info
          [:p.input-info
           "You can freely create any number of unique subnames from owned name. "
           "Each subname can be traded, transferred or pointed to an address, same way as any other ENS name."]
          [:p.input-info
           (when correct-subname?
             (str
               full-subname " will be created"))]]
         [:div.button
          [transaction-button
           {:primary true
            :disabled submit-disabled?
            :pending? @(subscribe [:ens.set-subnode-owner/tx-pending?
                                   (namehash full-name)
                                   (when subname (sha3 subname))])
            :pending-text "Creating subname..."
            :on-click #(dispatch [:ens/set-subnode-owner @form-data])}
           "Create Subname"]]]))))

(defn transfer-ownership-form [defaults]
  (let [form-data (r/atom (default-transfer-name-form-data defaults))]
    (fn []
      (let [{:keys [:ens.record/name :ens.record/owner]} @form-data
            ownership-status @(subscribe [:ens.record/ownership-status
                                          (when (seq name)
                                            (str name constants/registrar-root))])
            full-name (when (seq name)
                        (str name constants/registrar-root))
            submit-disabled? (or (not= ownership-status :ens.ownership-status/owner)
                                 (not (web3/address? owner)))
            top-level? (top-level-name? full-name)
            node-hash (sha3 name)
            registrar-entry @(subscribe [:name-bazaar-registrar/entry node-hash])]
        [:div.grid.submit-footer.offering-form
         [:div.name-ownership
          [ens-name-input-ownership-validated
           {:value name
            :on-change #(swap! form-data assoc :ens.record/name (aget %2 "value"))}]]
         [:div.address
          [address-text-field
           {:value owner
            :on-change #(swap! form-data assoc :ens.record/owner (aget %2 "value"))}]]
         [:div.info
          [:p.input-info
           "By transferring ownership you're giving full control over ENS name as well as its locked funds to a new owner."]
          (when-not submit-disabled?
            [:p.input-info
             owner " will become owner of the " full-name ","
             (when top-level?
               (str " as well as owner of the locked value "
                    (format-eth-with-code
                      (:name-bazaar-registrar.entry.deed/value registrar-entry))))])]
         [:div.button
          [transaction-button
           {:primary true
            :disabled submit-disabled?
            :pending? @(subscribe [:transfer-ownership/tx-pending? (namehash full-name) name top-level?])
            :pending-text "Transferring ownership..."
            :on-click #(dispatch [:name/transfer-ownership full-name owner])}
           "Transfer ownership"]]]))))

(defmethod page :route.user/manage-names []
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
          :address address}]]
       [ui/Segment
        [:h1.ui.header.padded "Create Subname"]
        [create-subname-form
         {:name (when (and name (valid-ens-name? name))
                  (strip-root-registrar-suffix (normalize name)))
          :address address}]]
       [ui/Segment
        [:h1.ui.header.padded "Transfer ownership"]
        [transfer-ownership-form
         {:name (when (and name (valid-ens-name? name))
                  (strip-root-registrar-suffix (normalize name)))
          :address address}]]])))
