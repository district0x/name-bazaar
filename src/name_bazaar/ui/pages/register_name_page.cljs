(ns name-bazaar.ui.pages.register-name-page
  (:require [cljs-time.core :as cljs-time]
            [clojure.string :as string]
            [clojure.set :as set]
            [district0x.shared.utils :as d0x-shared-utils]
            [district0x.ui.components.misc :as misc]
            [district0x.ui.components.input :as input]
            [district0x.ui.components.add-to-calendar :as add-to-calendar]
            [district0x.ui.components.transaction-button :as transaction-button]
            [district0x.ui.utils :as d0x-ui-utils]
            [goog.string :as gstring]
            [name-bazaar.shared.constants :as shared-constants]
            [name-bazaar.shared.utils :as nb-shared-utils]
            [name-bazaar.ui.components.app-layout :as app-layout]
            [name-bazaar.ui.components.ens-record.ens-name-input :as ens-name-input]
            [name-bazaar.ui.components.ens-record.etherscan-link :as ens-record-etherscan-link]
            [name-bazaar.ui.components.ens-record.general-info :as ens-record-general-info]
            [name-bazaar.ui.components.registrar-entry.general-info :as registrar-entry-general-info]
            [name-bazaar.ui.constants :as constants]
            [name-bazaar.ui.utils :as nb-ui-utils]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [soda-ash.core :as ui]
            [taoensso.timbre :as logging]))

(def ^:private label (r/atom ""))

(def registrar-auction-states
  ^{:doc "Higher weights are given higher priority in autosuggestions"}
  (merge #:registrar.entry.state{:loading {:text "Checking ownership..." :icon :loader :color :grey :status :grey :weight 1}
                                 :owned-phase-different-owner {:text "This name is owned" :icon :user2 :color :dark-blue :status :dark-blue :weight 2}
                                 :owned-phase-user-owner {:text "You are now the owner of this name" :icon :user2 :color :green :status :success :weight 3}
                                 :auction-user-made-bid {:text "You placed a bid for this name" :icon :hammer2 :color :yellow :status :warning :weight 4}
                                 :reveal-phase-user-outbid {:text "You have been outbid" :icon :times :color :red :status :failure :weight 5}
                                 :reveal-phase-user-winning {:text "Your bid is currently winning the auction" :icon :arrow-up :color :green :status :success :weight 6}
                                 :auction-no-user-made-bid {:text "This name is availiable for bids" :icon :check :color :green :status :success :weight 7}
                                 :owned-phase-user-owner-not-finalized {:text "Your bid has won the auction" :icon :user2 :color :green :status :success :weight 8}
                                 :reveal-phase-user-made-bid {:text "Your bid is waiting to be revealed" :icon :clock2 :color :yellow :status :warning :weight 9}
                                 :empty-name {:text "" :weight 0}
                                 :open {:text "This name is available for bids" :icon :check :color :green :status :success :weight 0}
                                 :reveal-phase-no-user-made-bid {:text "Bids are waiting to be revealed" :icon :times :color :yellow :status :warning :weight 0}
                                 :owned-phase-different-owner-not-finalized {:text "This name is owned" :icon :user2 :color :dark-blue :status :dark-blue :weight 0}}
    {::subname {:text "Subnames can't be registered" :color :red :status :failure :icon :times}
     ::invalid-length {:text "Only names with 7 and more characters can be registered" :color :yellow :status :warning :icon :times}
     ::start-auction-pending {:text "Opening..." :icon :loader :color :grey :status :grey :weight 1}
     ::start-auctions-and-bid-pending {:text "Bidding..." :icon :loader :color :grey :status :grey :weight 1}
     ::finalize-auction-pending {:text "Finalizing..." :icon :loader :color :grey :status :grey :weight 1}
     ::unseal-bid-pending {:text "Revealing..." :icon :loader :color :grey :status :grey :weight 1}}))

(def registration-bid-state->text
  #:registrar.entry.state{:reveal-phase-user-made-bid {:text "bids are waiting to be revealed"}
                          :owned-phase-user-owner-not-finalized {:text "bids are waiting for finalization"}
                          :auction-no-user-made-bid {:text "names are waiting for a bid"}
                          :reveal-phase-user-winning {:text "bids are winning"}})

(defn- supported-length? [name]
  (>= (count name) shared-constants/min-name-length))

(defn- top-level? [name]
  (nb-shared-utils/top-level-name? (str name constants/registrar-root)))

(defn load-bid-state [name]
  (when (and (supported-length? name)
          (top-level? name))
    (re-frame/dispatch [:registration-bids.state.ens-record/load name])))

(defn- ens-bid-info [state highest-bid owner]
  (fn [state highest-bid owner]
    [:div
     [:div.description "Highest revealed-bid: " highest-bid " ETH"]
     [:div.description.ellipsis "Winning bidder"
      (when (contains? #{:registrar.entry.state/reveal-phase-user-winning
                         :registrar.entry.state/owned-phase-user-owner} state) " (You)")
      ": "
      (if (nil? owner)
        "none"
        [:a {:href (nb-ui-utils/path-for :route.user/offerings {:user/address owner})}
         owner])]]))

(defn- description [state]
  (fn [state]
    (cond (contains? #{:registrar.entry.state/open
                       :registrar.entry.state/empty-name
                       ::invalid-length
                       ::subname
                       ::start-auction-pending
                       ::start-auctions-and-bid-pending} state)
          [:div.description
           [:p "In order to register an unclaimed name with the ENS, you’ll need to complete the following:"]
           [:ol
            [:li [:i "Open the auction"] " - This may also already be done by another person interested in the same name."]
            [:li [:i "Make a bid"] " - Once the auction is open, you have 72 hours to place your bid."]
            [:li [:i "Reveal your bid"] " - After the bid window, there is a reveal period of 48 hours. All bids must be revealed." [:font {:color :red} [:b " *Bids that are not revealed in this period cannot be refunded!*"]]]
            [:li [:i "Claim your name"] " - Upon conclusion of the reveal period, all losing *revealed* bids are refunded. The winner needs to finalize ownership of their new name. This refunds the difference between the winning bid and the next highest bid (or .01 ETH, if there are no other bids)."]]
           [:p "To get started, type a name you’re looking for into the search above and open the auction with or without a bid below."]
           [:p [:b "Note:"] " By opening an auction with the “Bid Now” button, you can combine step one and two above into a single transaction." [:b " This will allow others to see your bid early"] " and is not recommended if you’d like to keep your first bid a secret."]]

          (= :registrar.entry.state/auction-no-user-made-bid state)
          [:div.description "An auction has already started on this name. Place your bid below before the auction ends in order to claim this name. Don’t forget to return for the reveal phase in order to reveal your bid!"]

          (contains? #{:registrar.entry.state/auction-user-made-bid
                       ::unseal-bid-pending}
            state)
          [:div.description "Your bid has been placed, but still needs to be revealed. Return when the timer at the right reaches zero and the 48 hour reveal period begins in order to reveal your bids." [:font {:color :red} [:b " *Bids that are not revealed in this period cannot be refunded*!"]]])))

(defn- import-bids []
  (let [id :import-bids]
    [:label.description
     {:class :side-dark-shadowed}
     [:input {:type :file
              :on-change (fn [evt]
                           (d0x-ui-utils/handle-file-read evt
                             #(re-frame/dispatch [:registration-bids/import %])))}]
     [:b "IMPORT BIDS"]
     [:i.icon.hammer-arrow-left.green.big]]))

(defn- backup-bids [state]
  (fn [state]
    [:label.description
     {:class (if-not (= :registrar.entry.state/empty-name state)
               :side-dark-shadowed
               :dark-shadowed)
      :on-click (fn [evt]
                  (re-frame/dispatch [:registration-bids/save]))}
     [:b "BACKUP BIDS"]
     [:i.icon.hammer-arrow-right-left.violet.big]]))

(defn- remove-name [state]
  (fn [state]
    [:label.description
     {:class [(if (= :registrar.entry.state/empty-name state)
                :concealed
                :dark-shadowed)]
      :on-click (fn [evt]
                  (re-frame/dispatch [:registration-bids/remove (nb-ui-utils/sha3 @label)])
                  (reset! label ""))}
     [:b "REMOVE NAME"]
     [:i.icon.hammer-remove.red.big]]))

(defn- user-bids-buttons [state]
  (fn [state]
    [ui/ButtonGroup
     {:vertical true
      :fluid true}
     [import-bids]
     [backup-bids state]
     [remove-name state]]))

(defn auction-clock [label-hash state]
  (fn [label-hash state]
    (let [[time-remaining header] (cond
                                    (contains? #{:registrar.entry.state/auction-user-made-bid
                                                 :registrar.entry.state/auction-no-user-made-bid} state)
                                    [@(re-frame/subscribe [:registrar/bidding-time-remaining label-hash])
                                     "TIME REMAINING TO REVEAL PHASE"]

                                    (contains? #{:registrar.entry.state/reveal-phase-user-made-bid
                                                 :registrar.entry.state/reveal-phase-no-user-made-bid
                                                 :registrar.entry.state/reveal-phase-user-winning
                                                 :registrar.entry.state/reveal-phase-user-outbid
                                                 ::unseal-bid-pending} state)
                                    [@(re-frame/subscribe [:registrar/reveal-time-remaining label-hash])
                                     "TIME REMAINING TO THE END OF REVEAL PHASE"]

                                    (contains? #{:registrar.entry.state/owned-phase-user-owner-not-finalized
                                                 :registrar.entry.state/owned-phase-user-owner
                                                 :registrar.entry.state/owned-phase-different-owner
                                                 :registrar.entry.state/owned-phase-different-owner-not-finalized
                                                 ::finalize-auction-pending} state)
                                    [@(re-frame/subscribe [:registrar/ownership-time-remaining label-hash])
                                     "TIME REMAINING TO RENEWAL"])]
      [:div.auction-clock.shadowed
       [:div.ui.segment
        [:i.icon.clock]
        [:h5.ui.header.sub.centered  header]
        [:table
         [:tbody
          [:tr
           (for [unit [:days :hours :minutes :seconds]]
             (let [amount (get time-remaining unit 0)]
               [:td
                {:key unit}
                [:div {:class (when-not (= :seconds unit)
                                :right-bordered)}
                 [:div.amount amount]
                 [:div.time-unit (d0x-ui-utils/pluralize (d0x-ui-utils/time-unit->text unit) amount)]]]))]]]]])))

(defn auction-calendar [label-hash state]
  (fn [label-hash state]
    (let [enforce-utc (fn [s] (if (string/includes? s "Z")
                                s
                                (str s "Z")))
          [start-time end-time title description] (cond
                                                    (contains? #{:registrar.entry.state/auction-no-user-made-bid} state)
                                                    [@(re-frame/subscribe [:now])
                                                     @(re-frame/subscribe [:registrar/end-bidding-date label-hash])
                                                     (str "Make your bid - " @label)
                                                     "Don't let others win this name, make your bid before the auction ends"]

                                                    (contains? #{:registrar.entry.state/auction-user-made-bid} state)
                                                    [@(re-frame/subscribe [:registrar/end-bidding-date label-hash])
                                                     (:registrar.entry/registration-date @(re-frame/subscribe [:registrar/entry label-hash]))
                                                     (str "Reveal your ENS bid - " @label)
                                                     "Revealing your bid is necessary, otherwise you'll lose your funds"]

                                                    (contains? #{:registrar.entry.state/reveal-phase-user-made-bid
                                                                 :registrar.entry.state/reveal-phase-no-user-made-bid
                                                                 :registrar.entry.state/reveal-phase-user-outbid
                                                                 ::unseal-bid-pending} state)
                                                    [@(re-frame/subscribe [:now])
                                                     (:registrar.entry/registration-date @(re-frame/subscribe [:registrar/entry label-hash]))
                                                     (str "Reveal your ENS bid - " @label)
                                                     "Revealing your bid is necessary, otherwise you'll lose your funds"]

                                                    (contains? #{:registrar.entry.state/reveal-phase-user-winning} state)
                                                    (let [registration-date (:registrar.entry/registration-date @(re-frame/subscribe [:registrar/entry label-hash]))
                                                          end-ownership-date @(re-frame/subscribe [:registrar/end-ownership-date label-hash])]
                                                    [registration-date
                                                     end-ownership-date
                                                       (str "Finalize your ENS bid - " @label)
                                                       "You are currently winning the auction, come back later to finalize it"])

                                                    (contains? #{:registrar.entry.state/owned-phase-user-owner-not-finalized
                                                                 :registrar.entry.state/owned-phase-user-owner
                                                                 :registrar.entry.state/owned-phase-different-owner
                                                                 :registrar.entry.state/owned-phase-different-owner-not-finalized
                                                                 ::finalize-auction-pending} state)
                                                    (let [end-ownership-date @(re-frame/subscribe [:registrar/end-ownership-date label-hash])]
                                                      [end-ownership-date
                                                       end-ownership-date
                                                       (str "Renew name ownership - " @label)
                                                       "Renewing your ownership is necessary, otherwise you loose the name"]))]
      [:div.auction-calendar
       [add-to-calendar/add-to-calendar {:title title
                                         :url (str "https://namebazaar.io/register?name="
                                                @label constants/registrar-root)
                                         :description description
                                         :start-time (d0x-ui-utils/format-iso8601 start-time)
                                         :end-time (d0x-ui-utils/format-iso8601 end-time)}]])))

(defn- render-search-options [items]
  (let [query-for #(get-in registrar-auction-states [(keyword "registrar.entry.state" %1) %2])]
    (doall
      (map-indexed (fn [idx {:keys [:title :registration-bids/state]}]
                     {:key idx
                      :text title
                      :value title
                      :content (r/as-element [:div.dropdown-item
                                              [:div (str title constants/registrar-root)]
                                              [ui/Icon {:class (query-for state :icon)
                                                        :color (query-for state :color)
                                                        :circular true
                                                        :inverted true
                                                        :size :small}]])})
        items))))

(defn- search-bar [{:keys [:options :status :icon :text]}]
  (fn [{:keys [:options :status :icon :text]}]
    [:div.input-state-label
     {:class status}
     [:div.ui.fluid.action.labeled.input
      [ui/Dropdown
       {:fluid true
        :placeholder "Enter Name"
        :value @label
        :search-query @label
        :options (render-search-options options)
        :no-results-message nil
        :selection true
        :select-on-blur false
        :search true
        :on-focus (re-frame/dispatch [:registration-bids.states/load])
        :on-change (fn [_ data]
                     (let [v (aget data "value")]
                       (reset! label v)))
        :on-search-change (fn [_ data]
                            (let [v (-> data
                                      (aget "searchQuery")
                                      (clojure.string/trim))]
                              (reset! label v)))}]
      [:div.ui.label constants/registrar-root]]
     [:div.ui.label.top-padded (when icon
                                 [ui/Icon {:class icon
                                           :circular true
                                           :inverted true
                                           :size :small}])
      [:b.uppercase text]]]))

(defn middle-section [state & [opts]]
  (let [last-state (atom state)
        last-opts (atom opts)
        render (fn [state & [{:keys [:label-hash :registrar-state :highest-bid :owner] :as opts}]]
                 (cond
                   (contains? #{:registrar.entry.state/empty-name
                                :registrar.entry.state/open
                                :registrar.entry.state/subname
                                :registrar.entry.state/invalid-length
                                ::subname
                                ::invalid-length
                                ::start-auction-pending
                                ::start-auctions-and-bid-pending}
                     state)
                   [:div.grid.midsect.empty
                    [:div.info [description state]]]

                   (contains? #{:registrar.entry.state/auction-user-made-bid
                                :registrar.entry.state/auction-no-user-made-bid
                                ::unseal-bid-pending}
                     state)
                   [:div.grid.midsect.made-bid
                    [:b.description.underlined
                     [ens-record-etherscan-link/ens-record-etherscan-link
                      {:ens.record/name @label}]]
                    [:div.description [description state]]
                    [:div.clock
                     [auction-clock label-hash state]
                     [auction-calendar label-hash state]]]

                   (contains? #{:registrar.entry.state/owned-phase-user-owner
                                :registrar.entry.state/owned-phase-different-owner
                                :registrar.entry.state/owned-phase-different-owner-not-finalized} state)
                   [:div.grid.midsect.dif-owner
                    [:div.description
                     [ens-record-general-info/ens-record-general-info {:ens.record/name (str @label constants/registrar-root)}]
                     [registrar-entry-general-info/registrar-entry-general-info {:ens.record/name @label
                                                                                 :registrar.entry/state-text (get nb-ui-utils/registrar-entry-state->text registrar-state)}]]
                    [:div.clock
                     [auction-clock label-hash state]
                     [auction-calendar label-hash state]]]

                   (contains? #{:registrar.entry.state/reveal-phase-user-made-bid
                                :registrar.entry.state/reveal-phase-no-user-made-bid
                                :registrar.entry.state/reveal-phase-user-winning
                                :registrar.entry.state/owned-phase-user-owner-not-finalized
                                :registrar.entry.state/owned-phase-user-owner
                                ::finalize-auction-pending} state)
                   [:div.grid.midsect.reveal
                    [:div.bid-info
                     [ens-bid-info state highest-bid owner]]
                    [:div.clock
                     [auction-clock label-hash state]
                     [auction-calendar label-hash state]]]

                   (= :registrar.entry.state/reveal-phase-user-outbid state)
                   [:div.grid.midsect.reveal-outbid
                    [:b.description.underlined
                     [ens-record-etherscan-link/ens-record-etherscan-link
                      {:ens.record/name @label}]]
                    [:div.bid-info
                     [ens-bid-info state highest-bid owner]]
                    [:div.top-padded
                     [:div.description.warning "Unfortunately your bid isn't the highest for this name."]
                     [:div.description.warning [:b "99.5% of your bid was refunded to you."]]]
                    [:div.clock
                     [auction-clock label-hash state]
                     [auction-calendar label-hash state]]]))]
    (fn [state & [opts]]
      (if (= :registrar.entry.state/loading state)
        (render @last-state @last-opts)
        (do (reset! last-state state)
            (reset! last-opts opts)
            (render state opts))))))

(defn register-name-form []
  (let [last-state (atom :registrar.entry.state/empty-name)
        bid-value-input (r/atom "0.01")]
    (fn []
      (let [label-hash (nb-ui-utils/sha3 @label)
            start-auction-pending? @(re-frame/subscribe [:registrar.transact/tx-pending? :start-auction @label])
            start-auctions-and-bid-pending? @(re-frame/subscribe [:registrar.transact/tx-pending? :start-auctions-and-bid @label])
            finalize-auction-pending? @(re-frame/subscribe [:registrar.transact/tx-pending? :finalize-auction @label])
            unseal-bid-pending? @(re-frame/subscribe [:registrar.transact/tx-pending? :unseal-bid @label])
            [state registrar-state] (cond start-auction-pending?
                                          [::start-auction-pending nil]

                                          start-auctions-and-bid-pending?
                                          [::start-auctions-and-bid-pending nil]

                                          finalize-auction-pending?
                                          [::finalize-auction-pending nil]

                                          unseal-bid-pending?
                                          [::unseal-bid-pending nil]

                                          (and (not (empty? @label)) (not (top-level? @label)))
                                          [::subname nil]

                                          (and (not (empty? @label)) (not (supported-length? @label)))
                                          [::invalid-length nil]
                                          :else @(re-frame/subscribe [:registrar/auction-state label-hash]))
            {:keys [:registrar.entry/registration-date :registrar.entry/highest-bid :registrar.entry.deed/owner]} @(re-frame/subscribe [:registrar/entry label-hash])
            {:keys [:registrar/bid-salt :registrar/bid-value] :as bid} @(re-frame/subscribe [:registration-bid label-hash])
            bids-by-importance @(re-frame/subscribe [:registration-bids/user-bids-by-importance registrar-auction-states])
            most-important-state (-> bids-by-importance first :registration-bids/state)
            {:keys [:count]} @(re-frame/subscribe [:registration-bids/state-count most-important-state])
            fsm (cond (= :registrar.entry.state/empty-name state)
                      (when (and count (contains? (-> registration-bid-state->text keys set) most-important-state))
                        {:count count
                         :text (str count " " (get-in registration-bid-state->text [most-important-state :text]))
                         :color (get-in registrar-auction-states [most-important-state :color])
                         :status (get-in registrar-auction-states [most-important-state :status])
                         :icon (get-in registrar-auction-states [most-important-state :icon])})
                      :else (get registrar-auction-states state))
            status (:status fsm)
            icon (:icon fsm)
            text (:text fsm)
            show-bid-section? (contains? #{:registrar.entry.state/loading
                                           :registrar.entry.state/empty-name
                                           :registrar.entry.state/open
                                           :registrar.entry.state/auction-no-user-made-bid
                                           ::invalid-length
                                           ::subname
                                           ::start-auction-pending
                                           ::start-auctions-and-bid-pending} state)
            bid-disabled? #(or (empty? @label)
                               (contains? #{:registrar.entry.state/empty-name
                                            :registrar.entry.state/loading
                                            ::invalid-length
                                            ::subname
                                            ::start-auction-pending
                                            ::start-auctions-and-bid-pending} state))]
        [:div
         [:div.grid.register-name-page.submit-footer
          [:div.search-bar
           [search-bar {:options bids-by-importance
                        :status status :icon icon :text text}]]
          [:div.user-bids-button [user-bids-buttons state]]
          [:div.middle-section
           [middle-section state {:label-hash label-hash
                                  :registrar-state registrar-state
                                  :highest-bid highest-bid
                                  :owner owner}]]
          (when (not (contains? #{:registrar.entry.state/owned-phase-different-owner
                                  :registrar.entry.state/owned-phase-different-owner-not-finalized
                                  :registrar.entry.state/reveal-phase-user-outbid} state))
            [:div.bid-section.button
             (when show-bid-section?
               [:div.header "Your Bid"])
             [:div {:class (when show-bid-section?
                             :input-section)}
              (when show-bid-section?
                [input/token-input
                 {:value (or @bid-value-input 0.01)
                  :on-change #(reset! bid-value-input (aget %2 "value"))
                  :fluid true}
                 "Your bid"])
              (when (contains? #{:registrar.entry.state/auction-user-made-bid
                                 :registrar.entry.state/reveal-phase-user-made-bid
                                 :registrar.entry.state/reveal-phase-no-user-made-bid
                                 ::unseal-bid-pending} state)
                [transaction-button/transaction-button {:primary true
                                                        :disabled (not (= :registrar.entry.state/reveal-phase-user-made-bid state))
                                                        :pending? unseal-bid-pending?
                                                        :pending-text text
                                                        :on-click (fn []
                                                                    (re-frame/dispatch [:registrar/transact :unseal-bid {:registrar/label @label
                                                                                                                         :registrar/bid-value bid-value
                                                                                                                         :registrar/bid-salt bid-salt}])
                                                                    (load-bid-state @label))}
                 "Reveal Bid"])
              (when (contains? #{:registrar.entry.state/reveal-phase-user-winning
                                 :registrar.entry.state/owned-phase-user-owner-not-finalized
                                 ::finalize-auction-pending} state)
                [transaction-button/transaction-button {:primary true
                                                        :disabled (not (= :registrar.entry.state/owned-phase-user-owner-not-finalized state))
                                                        :pending? finalize-auction-pending?
                                                        :pending-text text
                                                        :on-click (fn []
                                                                    (re-frame/dispatch [:registrar/transact :finalize-auction {:registrar/label @label}])
                                                                    (load-bid-state @label))}
                 "Finalize Bid"])
              (when (= :registrar.entry.state/owned-phase-user-owner state)
                [transaction-button/transaction-button {:primary true
                                                        :disabled (not (= :registrar.entry.state/owned-phase-user-owner state))
                                                        :on-click (fn []
                                                                    (re-frame/dispatch [:district0x.location/set-query-and-nav-to :route.offerings/create
                                                                                        {:ens.record/name (str @label constants/registrar-root)}
                                                                                        constants/routes]))}
                 "Create Offering"])
              (when (contains? #{:registrar.entry.state/loading
                                 :registrar.entry.state/empty-name
                                 :registrar.entry.state/open
                                 :registrar.entry.state/auction-no-user-made-bid
                                 ::invalid-length
                                 ::subname
                                 ::start-auctions-and-bid-pending} state)
                [transaction-button/transaction-button
                 {:primary true
                  :disabled (bid-disabled?)
                  :pending? start-auctions-and-bid-pending?
                  :pending-text text
                  :on-click (fn []
                              (cond
                                (= state :registrar.entry.state/open)
                                (re-frame/dispatch [:registrar/transact :start-auctions-and-bid {:registrar/label @label
                                                                                                 :registrar/bid-value @bid-value-input}])

                                (= state :registrar.entry.state/auction-no-user-made-bid)
                                (re-frame/dispatch [:registrar/transact :new-bid {:registrar/label @label
                                                                                  :registrar/bid-value @bid-value-input}])

                                :else (logging/error "Unknown state" {:state state} ::transaction-button))
                              (load-bid-state @label))}
                 "Bid Now"])
              (when (contains? #{:registrar.entry.state/loading
                                 :registrar.entry.state/open
                                 :registrar.entry.state/empty-name
                                 ::invalid-length
                                 ::subname
                                 ::start-auction-pending
                                 ::start-auctions-and-bid-pending} state)
                [transaction-button/transaction-button {:color :purple
                                                        :disabled (bid-disabled?)
                                                        :pending? start-auction-pending?
                                                        :pending-text text
                                                        :on-click (fn []
                                                                    (re-frame/dispatch [:registrar/transact :start-auction {:registrar/label @label}])
                                                                    (load-bid-state @label))}
                 "Open Without Bid"])]])]]))))

(defmethod misc/page :route.registrar/register []
  (let [{:keys [:name] :as query-params} @(re-frame/subscribe [:district0x/query-params])
        watch-fn (fn [] (fn [_key _ref _old-name new-name]
                          (load-bid-state new-name)))]
    (fn []
      (add-watch label :label-watcher (watch-fn))
      (if (and name (nb-ui-utils/valid-ens-name? name))
        (reset! label (nb-ui-utils/strip-root-registrar-suffix (nb-ui-utils/normalize name)))
        (reset! label ""))
      [app-layout/app-layout {:meta {:title "NameBazaar - Register ENS Name"
                                     :description "Simplest way to register a new ENS name."}}
       [ui/Segment
        [:h1.ui.header.padded "Register Name"]
        [register-name-form]]])))
