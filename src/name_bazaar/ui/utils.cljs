(ns name-bazaar.ui.utils
  (:require
    [cemerick.url :as url]
    [clojure.data :as data]
    [clojure.string :as string]
    [cljs-web3.core :as web3]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.history :as history]
    [district0x.ui.utils :as d0x-ui-utils :refer [truncate path-with-query]]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.shared.utils :refer [name-label]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.db :refer [default-db]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(defn namehash [name]
  (js/EthEnsNamehash.hash name))

(defn normalize [name]
  (js/EthEnsNamehash.normalize name))

(defn sha3 [x]
  (str "0x" (js/keccak_256 x)))

(def name->label-hash (comp sha3 name-label))

(defn valid-ens-name? [name]
  (try
    (normalize name)
    true
    (catch js/Error e
      false)))

(defn ensure-registrar-root [name]
  (if-not (string/ends-with? name constants/registrar-root)
    (str name constants/registrar-root)
    name))

(defn strip-root-registrar-suffix [s]
  (if (and (string? s) (string/ends-with? s constants/registrar-root))
    (subs s 0 (- (count s) 4))
    s))

(defn parse-query-params [query-params route-key]
  (d0x-shared-utils/apply-parsers query-params (constants/query-params-parsers route-key)))

(defn path-for [route & [route-params]]
  (history/path-for {:route route
                     :route-params route-params
                     :routes constants/routes}))

(def offerings-newest-url (path-with-query (path-for :route.offerings/search)
                                           {:order-by-columns [(name :created-on)]
                                            :order-by-dirs [(name :desc)]}))

(def offerings-most-active-url (path-with-query (path-for :route.offerings/search)
                                                {:order-by-columns [(name :bid-count)]
                                                 :order-by-dirs [(name :desc)]}))

(def offerings-ending-soon-url (path-with-query (path-for :route.offerings/search)
                                                {:order-by-columns [(name :end-time)]
                                                 :order-by-dirs [(name :asc)]}))

(def offerings-sold-query-params {:sold? true
                                  :order-by-columns [(name :finalized-on)]
                                  :order-by-dirs [(name :desc)]})

(def offerings-sold-url (path-with-query (path-for :route.offerings/search)
                                         offerings-sold-query-params))

(defn etherscan-ens-url [name]
  (gstring/format "https://etherscan.io/enslookup?q=%s" name))

(def offering-type->text
  {:buy-now-offering "Buy Now"
   :auction-offering "Auction"})

(def offering-type->icon
  {:buy-now-offering "bag"
   :auction-offering "hammer"})

(def offering-status->text
  {:offering.status/emergency "Emergency Cancel"
   :offering.status/active "Active"
   :offering.status/finalized "Completed"
   :offering.status/missing-ownership "Missing Ownership"
   :offering.status/auction-ended "Auction Ended"})

(defn get-ens-record [db node]
  (get-in db [:ens/records node]))

(defn get-ens-record-name [db node]
  (get-in db [:ens/records node :ens.record/name]))

(defn get-ens-record-active-offering [db node]
  (get-in db [:ens/records node :ens.record/active-offering]))

(defn get-offering-name [db offering-address]
  (get-in db [:offerings offering-address :offering/name]))

(defn get-offering [db offering-address]
  (get-in db [:offerings offering-address]))

(defn get-offering-search-results [db search-results-key]
  (get-in db [:search-results :offerings search-results-key]))

(defn get-offering-requests-search-results [db search-results-key]
  (get-in db [:search-results :offering-requests search-results-key]))

(defn get-similar-offering-pattern [{:keys [:offering/label :offering/name :offering/node :offering/top-level-name?]}]
  (let [subnames (if top-level-name?
                   ""
                   (-> name
                     (string/replace (str label ".") "")
                     (string/replace constants/registrar-root "")))]
    (str (subs label 0 3) "%" subnames)))

(defn update-search-results-params [db params-db-path new-params {:keys [:append? :reset-params?]}]
  (let [default-search-params (get-in default-db params-db-path)
        search-params (cond-> default-search-params
                        (not reset-params?) (merge (get-in db params-db-path))
                        (not append?) (merge (select-keys default-search-params [:offset :limit]))
                        true (merge new-params))]
    {:db (cond-> db
           (not append?) (assoc-in [:infinite-list :expanded-items] {})
           true (assoc-in params-db-path search-params))
     :search-params search-params}))

(defn registrar-entry-deed-loaded? [registrar-entry]
  (boolean (or (d0x-shared-utils/zero-address? (:registrar.entry.deed/address registrar-entry))
               (and (:registrar.entry.deed/value registrar-entry)
                    (:registrar.entry.deed/owner registrar-entry)))))

(defn ens-record-loaded? [ens-record]
  (boolean (:ens.record/owner ens-record)))

(def registrar-entry-state->text
  {:registrar.entry.state/open "Open For Bids"
   :registrar.entry.state/auction "Initial Auction Ongoing"
   :registrar.entry.state/owned "Owned"
   :registrar.entry.state/forbidden "Forbidden"
   :registrar.entry.state/reveal "Reveal Period"
   :registrar.entry.state/not-yet-available "Not Yet Available"})

(defn debounce?
  "if the newly changed params are exactly one of expected ks"
  [old new ks]
  (let [changed-keys (-> (data/diff old new)
                       second
                       keys)]
    (and (= (count changed-keys) 1)
         (contains? (set ks) (first changed-keys)))))

(defn ensure-registrar-root-suffix [name]
  (when name
    (if (string/ends-with? name constants/registrar-root)
      name
      (str name constants/registrar-root))))

(defn resolve-address [db name]
  (if-not (web3/address? name)
    (let [addr-patched (ensure-registrar-root-suffix name)]
      (get-in db [:public-resolver/records (namehash addr-patched) :public-resolver.record/addr]))
    name))

(defn reverse-resolve-address [reverse-records addr]
  (when (web3/address? addr)
    (get-in reverse-records [addr :public-resolver.record/name])))

(defn user-name [name-or-addr & [trunc]]
  (if (web3/address? name-or-addr)
    (truncate name-or-addr (or trunc 10))
    (strip-root-registrar-suffix name-or-addr)))

(defn reverse-record-node [addr]
  (namehash (str (apply str (drop 2 addr))
                 ".addr.reverse")))
