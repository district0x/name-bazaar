(ns name-bazaar.ui.events.registration-bids-events
  (:require [clojure.walk :as walk]
            [clojure.set :as set]
            [cljs.spec.alpha :as s]
            [cljs-web3.core :as web3]
            [district0x.shared.utils :as d0x-shared-utils]
            [district0x.ui.events :as d0x-events]
            [district0x.ui.interceptors :as interceptors]
            [district0x.ui.utils :as d0x-ui-utils]
            [name-bazaar.ui.constants :as constants]
            [name-bazaar.ui.spec :as spec]
            [name-bazaar.ui.subs.registration-bids-subs :as registration-bids-subs]
            [name-bazaar.ui.utils :as nb-ui-utils]
            [medley.core :as medley]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as logging]))

(re-frame/reg-event-fx
 :registration-bids.state.ens-record/load
 [constants/interceptors]
 (fn [{:keys [:db]} [name]]
   (let [label-hash (nb-ui-utils/sha3 name)]
     {:async-flow {:first-dispatch [:registration-bids.state/load label-hash]
                   :rules [{:when :seen?
                            :events [:registration-bids.state/update]
                            :dispatch [:registration-bids.ens-record/load label-hash
                                       (nb-ui-utils/namehash (str name constants/registrar-root))]}]}})))

(re-frame/reg-event-fx
 :registration-bids.ens-record/load
 [constants/interceptors]
 (fn [{:keys [:db]} [label-hash name-hash]]
   (let [registrar-state (get-in db [:registrar/entries label-hash :registrar.entry/state])]
     (when (= :registrar.entry.state/owned registrar-state)
       {:dispatch [:ens.records/load [name-hash] {:load-resolver? true}]}))))

(re-frame/reg-event-fx
 :registration-bids.state/load
 [constants/interceptors (interceptors/inject-sub [:active-address] :user)]
 (fn [{:keys [:db :user]} [label-hash]]
   ^{:doc "Load all registrar information about the user bid with `:registrar.entries/load`.
This event can terminate with `:registrar.entry/loaded` or `:registrar-entry.deed.owner/loaded`
so we dispatch on both of these events."}
   {:async-flow {:first-dispatch [:registrar.entries/load [label-hash]]
                 :rules [{:when :seen-any-of?
                          :events [:registrar.entry/loaded
                                   :registrar-entry.deed.owner/loaded]
                          :dispatch [:registration-bids.bid-unsealed?/load label-hash]}]}}))

(re-frame/reg-event-fx
 :registration-bids.states/load
 [constants/interceptors (interceptors/inject-sub [:active-address] :user)]
 (fn [{:keys [:db :user]} _]
   {:dispatch-n (mapv #(vec [:registration-bids.state/load %])
                      (conj (-> (get-in db [:registration-bids user]) keys vec)))}))

(re-frame/reg-event-fx
 :registration-bids.bid-unsealed?/load
 [constants/interceptors (interceptors/inject-sub [:active-address] :user)]
 (fn [{:keys [:db :user]} [label-hash]]
   (let [{:keys [:registrar/bid-salt :registrar/bid-value]} (get-in db [:registration-bids user label-hash])
         sealed-bid (nb-ui-utils/seal-bid label-hash user (js/parseInt (web3/to-wei bid-value :ether)) (web3/sha3 bid-salt))]
     {:web3-fx.contract/constant-fns
      {:fns [{:instance (d0x-events/get-instance db :registrar)
              :method :sealed-bids
              :args [user sealed-bid]
              :on-success [:registration-bids.bid-unsealed?/update label-hash]
              :on-error [:district0x.log/error]}]}})))

(re-frame/reg-event-fx
 :registration-bids.bid-unsealed?/update
 [constants/interceptors (interceptors/inject-sub [:active-address] :user)]
 (fn [{:keys [:db :user]} [label-hash deed-address]]
   {:db (d0x-ui-utils/safe-assoc-in db [:registration-bids user label-hash :registration-bids/bid-unsealed?]
                                    (= d0x-shared-utils/zero-address deed-address))
    :dispatch [:registration-bids.state/update user label-hash]}))

(re-frame/reg-event-fx
 :registration-bids.state/update
 [constants/interceptors]
 (fn [{:keys [:db]} [user label-hash]]
   {:db (d0x-ui-utils/safe-assoc-in db [:registration-bids user label-hash :registration-bids/state]
                                    (first (registration-bids-subs/query-registrar-auction-state db user label-hash)))}))

(re-frame/reg-event-fx
 :registration-bids.localstorage/persist
 [constants/interceptors (interceptors/inject-sub [:registration-bids] :bids) (re-frame/inject-cofx :localstorage)]
 (fn [{:keys [:bids :localstorage]}]
   {:localstorage (assoc-in localstorage [:registration-bids] bids)}))

(re-frame/reg-event-fx
 :registration-bids/add
 [constants/interceptors]
 (fn [{:keys [:db]} [{:keys [:registrar/label :registrar/label-hash :registrar/bidder
                             :registrar/bid-salt :registrar/bid-value]}]]
   {:db (assoc-in db [:registration-bids bidder label-hash] {:registrar/label label
                                                             :registrar/bid-salt bid-salt
                                                             :registrar/bid-value bid-value})
    :dispatch-n [[:registration-bids.localstorage/persist]
                 [:registration-bids.state/load label-hash]]}))

(re-frame/reg-event-fx
 :registration-bids/save
 [constants/interceptors
  (interceptors/inject-sub [:now] :now)
  (interceptors/inject-sub [:registration-bids] :bids)]
 (fn [{:keys [:bids :now]} [{:keys [:file/filename]
                             :or {filename (str "namebazaar_bids_" now ".json")}}]]
   (let [subset #{:registrar/label :registrar/bid-salt :registrar/bid-value}]
     {:file/write [filename (d0x-shared-utils/clj->json (walk/postwalk (fn [el]
                                                                         (if (and (map? el)
                                                                                  (set/subset? subset
                                                                                               (-> el keys set)))
                                                                           (select-keys el subset)
                                                                           el))
                                                                       bids))]})))

(re-frame/reg-event-fx
 :registration-bids/import
 [constants/interceptors (interceptors/inject-sub [:registration-bids] :bids)]
 (fn [{:keys [:db :bids]} [content]]
   (let [new-bids (walk/postwalk #(case %
                                    "label"
                                    :registrar/label
                                    "bid-salt"
                                    :registrar/bid-salt
                                    "bid-value"
                                    :registrar/bid-value
                                    %)
                                 (d0x-shared-utils/json->clj content))]
     (if (s/valid? ::spec/registration-bids new-bids)
       {:db (assoc db :registration-bids
                   (d0x-shared-utils/merge-in bids new-bids))
        :dispatch-n [[:registration-bids.localstorage/persist]
                     [:registration-bids.states/load]]}
       (logging/warn "Wrong bids format" {:error (s/explain-str ::spec/registration-bids new-bids)} ::registration-bids-import) ))))

(re-frame/reg-event-fx
 :registration-bids/remove
 [constants/interceptors (interceptors/inject-sub [:active-address] :user)]
 (fn [{:keys [:db :user]} [label-hash]]
   {:db (update-in db [:registration-bids user] dissoc label-hash)
    :dispatch [:registration-bids.localstorage/persist]}))
