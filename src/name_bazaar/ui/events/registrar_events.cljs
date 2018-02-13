(ns name-bazaar.ui.events.registrar-events
  (:require
    [bignumber.core :as bn]
    [cljs.spec.alpha :as s]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [district0x.shared.utils :refer [wei->eth->num eth->wei empty-address? rand-str zero-address]]
    [district0x.ui.events :as d0x-ui-events]
    [district0x.ui.spec-interceptors :refer [validate-first-arg]]
    [district0x.ui.utils :as d0x-ui-utils]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [parse-registrar-entry]]
    [name-bazaar.ui.constants :as constants :refer [default-gas-price interceptors]]
    [name-bazaar.ui.utils :refer [sha3 seal-bid normalize path-for]]
    [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path after dispatch trim-v console]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(reg-event-fx
  :registrar/transfer
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label :ens.record/owner]))]
  (fn [{:keys [:db]} [form-data opts]]
    (let [form-data (assoc form-data :ens.record/label-hash (sha3 (:ens.record/label form-data)))
          name (str (:ens.record/label form-data) constants/registrar-root)]
      {:dispatch [:district0x/make-transaction
                  (merge
                   {:name (gstring/format "Transfer %s ownership" name)
                    :contract-key :registrar
                    :contract-method :transfer
                    :form-data form-data
                    :args-order [:ens.record/label-hash :ens.record/owner]
                    :form-id (select-keys form-data [:ens.record/label])
                    :tx-opts {:gas 75000 :gas-price default-gas-price}}
                   opts)]})))

(reg-event-fx
  :registrar/register
  [interceptors (validate-first-arg (s/keys :req [:ens.record/label]))]
  (fn [{:keys [:db]} [form-data]]
    (let [label (normalize (:ens.record/label form-data))
          ens-record-name (str label constants/registrar-root)
          form-data (assoc form-data :ens.record/label-hash (sha3 label))]
      {:dispatch [:district0x/make-transaction
                  {:name (gstring/format "Register %s" ens-record-name)
                   :contract-key :registrar
                   :contract-method :register
                   :form-data form-data
                   :result-href (path-for :route.ens-record/detail {:ens.record/name ens-record-name})
                   :args-order [:ens.record/label-hash]
                   :tx-opts {:gas 700000 :gas-price default-gas-price}
                   :on-tx-receipt [:district0x.snackbar/show-message
                                   (gstring/format "%s was successfully registered" ens-record-name)]}]})))

(defn- registrar-transaction [method {:keys [:registrar/label :registrar/bidder :registrar/bid-salt
                                             :registrar/bid-value :registrar/bid-mask]}]
  {:pre [(cond
           (contains? #{:start-auction :finalize-auction} method)
           (and label bidder)

           (contains? #{:new-bid :start-auctions-and-bid} method)
           (and label bidder bid-salt bid-value bid-mask)

           (= method :unseal-bid)
           (and label bid-salt bid-value))]}
  (let [normalized-label (normalize label)
        label-hash (web3/sha3 normalized-label)
        ens-record-name (str normalized-label constants/registrar-root)
        sealed-bid (when (contains? #{:new-bid :start-auctions-and-bid :unseal-bid} method)
                     (seal-bid label-hash bidder (js/parseInt (web3/to-wei bid-value :ether)) (web3/sha3 bid-salt)))]
    {:name (case method
             :start-auction
             (gstring/format "Open registration for %s" ens-record-name)

             :new-bid
             (gstring/format "Bid for %s" ens-record-name)

             :start-auctions-and-bid
             (gstring/format "Open registration and bid for %s" ens-record-name)

             :unseal-bid
             (gstring/format "Reveal bid for %s" ens-record-name)

             :finalize-auction
             (gstring/format "Finalize auction for %s" ens-record-name))
     :contract-key :registrar
     :contract-method method
     :form-id {:registrar/label label}
     :form-data (cond
                  (contains? #{:start-auction :finalize-auction} method)
                  {:registrar/label-hash label-hash}

                  (contains? #{:new-bid :start-auctions-and-bid} method)
                  (merge {:registrar/sealed-bid sealed-bid}
                         (when (= :start-auctions-and-bid method)
                           {:registrar/label-hashes [label-hash]}))

                  (= method :unseal-bid)
                  {:registrar/label-hash label-hash
                   :registrar/bid-value (js/parseInt (web3/to-wei bid-value :ether))
                   :registrar/bid-salt (web3/sha3 bid-salt)})

     :result-href (d0x-ui-utils/path-with-query (path-for :route.registrar/register)
                                                {:name ens-record-name})

     :args-order (cond
                   (contains? #{:start-auction :finalize-auction} method)
                   [:registrar/label-hash]

                   (contains? #{:new-bid :start-auctions-and-bid} method)
                   (remove nil? [(when (= :start-auctions-and-bid method)
                                   :registrar/label-hashes)
                                 :registrar/sealed-bid])

                   (= method :unseal-bid)
                   [:registrar/label-hash :registrar/bid-value
                    :registrar/bid-salt])
     :tx-opts (merge {:gas 700000 :gas-price default-gas-price}
                     (when (contains? #{:new-bid :start-auctions-and-bid} method)
                       {:value (web3/to-wei (+ bid-mask bid-value) :ether)}))
     :on-tx-receipt-n (cond
                        (= :start-auction method)
                        [[:district0x.snackbar/show-message
                          (gstring/format "%s registration successfully opened"
                                          ens-record-name)]]

                        (= :finalize-auction method)
                        [[:district0x.snackbar/show-message
                          (gstring/format "%s auction successfully finalized"
                                          ens-record-name)]]

                        (= :unseal-bid method)
                        [[:district0x.snackbar/show-message
                          (gstring/format "Bid for %s successfully revealed"
                                          ens-record-name)]]

                        (contains? #{:new-bid :start-auctions-and-bid} method)
                        [(if (= :start-auctions-and-bid method)
                           [:district0x.snackbar/show-message
                            (gstring/format " %s registration successfully opened and bid placed" ens-record-name)]
                           [:district0x.snackbar/show-message
                            (gstring/format "Bid successfully placed for %s" ens-record-name)])])}))

(reg-event-fx
  :registrar/transact
  [interceptors (validate-first-arg (s/and keyword?
                                      #(contains? #{:start-auction :start-auctions-and-bid :new-bid
                                                    :unseal-bid :finalize-auction} %)))]
  (fn [{:keys [:db]} [method form-data]]
    (let [{:keys [:registrar/label :registrar/bid-value
                  :registrar/bidder :registrar/bid-salt :registrar/bid-mask]
           :or {bidder (:active-address db) bid-salt (rand-str 10) bid-mask 0}} form-data
          normalized-label (normalize label)
          label-hash (web3/sha3 normalized-label)]
      {:dispatch-n (remove nil? [(when (= :start-auction method)
                                   [:registration-bids/add {:registrar/label-hash label-hash :registrar/bidder bidder
                                                            :registrar/label label}])
                                 (when (contains? #{:new-bid :start-auctions-and-bid} method)
                                   [:registration-bids/add {:registrar/label-hash label-hash :registrar/bidder bidder
                                                            :registrar/bid-salt bid-salt :registrar/bid-value bid-value
                                                            :registrar/label label}])
                                 [:district0x/make-transaction (registrar-transaction method (merge {:registrar/label label :registrar/bidder bidder}
                                                                                               (when (contains? #{:new-bid :start-auctions-and-bid :unseal-bid} method)
                                                                                                 {:registrar/bid-value bid-value
                                                                                                  :registrar/bid-salt bid-salt})
                                                                                               (when (contains? #{:new-bid :start-auctions-and-bid} method)
                                                                                                 {:registrar/bid-mask bid-mask})))]])})))

(reg-event-fx
  :registrar.entries/load
  interceptors
  (fn [{:keys [:db]} [label-hashes]]
    (let [instance (d0x-ui-events/get-instance db :registrar)]
      {:web3-fx.contract/constant-fns
       {:fns (for [label-hash label-hashes]
               {:instance instance
                :method :entries
                :args [label-hash]
                :on-success [:registrar.entry/loaded label-hash]
                :on-error [:district0x.log/error]})}})))

(reg-event-fx
  :registrar.entry/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash registrar-entry]]
    (let [registrar-entry (parse-registrar-entry registrar-entry {:parse-dates? true :convert-to-ether? true})]
      {:db (update-in db [:registrar/entries label-hash] merge registrar-entry)
       :dispatch [:registrar.entry.deed/load label-hash]})))

(reg-event-fx
  :registrar.entry.deed/load
  interceptors
  (fn [{:keys [:db]} [label-hash]]
    (let [deed-address (get-in db [:registrar/entries label-hash :registrar.entry.deed/address])]
      (when-not (empty-address? deed-address)
        {:web3-fx.contract/constant-fns
         {:fns [{:instance (d0x-ui-events/get-instance db :deed deed-address)
                 :method :value
                 :on-success [:registrar-entry.deed.value/loaded label-hash]
                 :on-error [:district0x.log/error]}
                {:instance (d0x-ui-events/get-instance db :deed deed-address)
                 :method :owner
                 :on-success [:registrar-entry.deed.owner/loaded label-hash]
                 :on-error [:district0x.log/error]}]}}))))

(reg-event-fx
  :registrar-entry.deed.value/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash deed-value]]
    {:db (assoc-in db
                   [:registrar/entries label-hash :registrar.entry.deed/value] (wei->eth->num deed-value))}))

(reg-event-fx
  :registrar-entry.deed.owner/loaded
  interceptors
  (fn [{:keys [:db]} [label-hash deed-owner]]
    {:db (assoc-in db [:registrar/entries label-hash :registrar.entry.deed/owner] deed-owner)}))
