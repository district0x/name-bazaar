(ns name-bazaar.ui.utils
  (:require
    [clojure.string :as string]
    [district0x.shared.utils :as d0x-shared-utils]
    [district0x.ui.utils :as d0x-ui-utils]
    [goog.string :as gstring]
    [goog.string.format]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.shared.utils :refer [name-label]]))

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

(defn strip-eth-suffix [s]
  (if (and (string? s) (string/ends-with? s ".eth"))
    (subs s 0 (- (count s) 4))
    s))

(defn parse-query-params [query-params route-key]
  (d0x-shared-utils/apply-parsers query-params (constants/query-params-parsers route-key)))

(defn path-for [route route-params]
  (d0x-ui-utils/path-for {:route route
                          :route-params route-params
                          :routes constants/routes}))

(defn etherscan-ens-url [name]
  (gstring/format "https://etherscan.io/enslookup?q=%s" name))

(def offering-type->text
  {:buy-now-offering "Buy Now"
   :auction-offering "Auction"})

(defn get-node-name [db node]
  (get-in db [:ens/records node :ens.record/name]))

(defn get-offering-name [db offering-address]
  (get-in db [:offerings offering-address :offering/name]))

(defn get-offering [db offering-address]
  (get-in db [:offerings offering-address]))

(defn get-offering-search-results [db search-results-key]
  (get-in db [:search-results :offerings search-results-key]))

(defn get-offering-requests-search-results [db search-results-key]
  (get-in db [:search-results :offering-requests search-results-key]))

(defn registrar-entry-deed-loaded? [registrar-entry]
  (boolean (or (d0x-shared-utils/zero-address? (:registrar.entry.deed/address registrar-entry))
               (:registrar.entry.deed/value registrar-entry))))

(defn ens-record-loaded? [ens-record]
  (boolean (:ens.record/owner ens-record)))

(def registrar-entry-state->text
  {:registrar.entry.state/open "Open For Bids"
   :registrar.entry.state/auction "Initial Auction Ongoing"
   :registrar.entry.state/owned "Owned"
   :registrar.entry.state/forbidden "Forbidden"
   :registrar.entry.state/reveal "Reveal Period"
   :registrar.entry.state/not-yet-available "Not Yet Available"})