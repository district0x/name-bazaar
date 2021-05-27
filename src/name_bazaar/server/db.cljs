(ns name-bazaar.server.db
  (:require
    [cljs-time.coerce :refer [to-epoch]]
    [cljs-time.core :as t]
    [district.server.config :refer [config]]
    [district.server.db :as db]
    [district0x.shared.utils :refer [combination-of? collify]]
    [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
    [name-bazaar.shared.utils :refer [emergency-state-new-owner unregistered-new-owner unregistered-price-wei]]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} name-bazaar-db
          :start (start (merge (:name-bazaar/db @config)
                               (:name-bazaar/db (mount/args))))
          :stop (stop))

(def offering-columns
  [[:offering/address address primary-key not-nil]
   [:offering/created-on :unsigned :integer not-nil]
   [:offering/node sha3-hash not-nil]
   [:offering/name :varchar not-nil]
   [:offering/original-owner address not-nil]
   [:offering/new-owner address default-nil]
   [:offering/version :unsigned :integer not-nil]
   [:offering/price :unsigned :BIG :INT not-nil]
   [:offering/finalized-on :unsigned :integer default-nil]
   [:offering/name-level :unsigned :integer default-zero]
   [:offering/label-length :unsigned :integer default-zero]
   [:offering/contains-number? :boolean not-nil default-false]
   [:offering/contains-special-char? :boolean not-nil default-false]
   [:offering/node-owner? :boolean not-nil default-false]
   [:offering/buy-now? :boolean not-nil default-false]
   [:offering/auction? :boolean not-nil default-false]
   [:offering/top-level-name? :boolean not-nil default-false]
   [:auction-offering/end-time :unsigned :integer default-nil]
   [:auction-offering/bid-count :unsigned :integer default-zero]
   [:auction-offering/winning-bidder address default-nil]])

(def offering-bids-columns
  [[:bid/bidder address not-nil]
   [:bid/value :unsigned :integer not-nil]
   [:bid/offering address not-nil]
   [(sql/call :foreign-key :bid/offering) (sql/call :references :offerings :offering/address)]])

(def offering-column-names (map first offering-columns))
(def offering-bids-column-names (filter keyword? (map first offering-bids-columns)))


(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))


(defn start [opts]
  (db/run! {:create-table [:offerings]
            :with-columns [offering-columns]})

  (db/run! {:create-table [:offering/bids]
            :with-columns [offering-bids-columns]})

  (doseq [column (rest offering-column-names)]
    (db/run! {:create-index (index-name column) :on [:offerings column]}))

  (doseq [column [:bid/bidder :bid/value]]
    (db/run! {:create-index (index-name column) :on [:offering/bids column]})))


(defn stop []
  (db/run! {:drop-table [:offering/bids]})
  (db/run! {:drop-table [:offerings]}))


(defn upsert-offering! [values]
  (db/run! {:insert-or-replace-into :offerings
            :columns offering-column-names
            :values [((apply juxt offering-column-names) values)]}))


(defn set-offering-node-owner?! [{:keys [:offering/node-owner? :offering/address]}]
  (db/run! {:update :offerings
            :set {:offering/node-owner? node-owner?}
            :where [:= :offering/address address]}))


(defn offering-exists? [offering-address]
  (boolean (seq (db/get {:select [1]
                         :from [:offerings]
                         :where [:= :offering/address offering-address]}))))


(defn insert-bid! [values]
  (db/run! {:insert-into :offering/bids
            :columns offering-bids-column-names
            :values [((apply juxt offering-bids-column-names) values)]}))


(defn- name-pattern [name name-position]
  (condp = name-position
    :any (str "%" name "%")
    :start (str name "%")
    :end (str "%" name)
    (str "%" name "%")))


(defn prepare-order-by [order-by order-by-dir {:keys [:name :root-name]}]
  (condp = [order-by order-by-dir]
    [:auction-offering/end-time :asc] [(sql/call :ifnull :auction-offering/end-time js/Number.MAX_VALUE) :asc] ;; Put nulls at the end
    [:name-relevance :desc] (db/order-by-similarity :offering/name name {:suffix (str "." root-name)})
    [order-by order-by-dir]))


(def unregistered-offerings-where-query
  [:or
   [:and [:or
          [:= :offering/version 1]
          [:= :offering/version 100000]]
    [:<> :offering/price unregistered-price-wei]]
   [:and
    [:not= :offering/version 1]
    [:not= :offering/version 100000]
    [:or
     [:not= :offering/new-owner unregistered-new-owner]
     [:= :offering/new-owner nil]]]])


(defn get-offerings [{:keys [:original-owner :new-owner :node :nodes :name :min-price :max-price :buy-now? :auction?
                             :min-length :max-length :name-position :min-end-time-now? :version :node-owner?
                             :top-level-names? :sub-level-names? :exclude-node :exclude-special-chars?
                             :exclude-numbers? :limit :offset :order-by :order-by-dir fields :root-name
                             :total-count? :bidder :winning-bidder :exclude-winning-bidder :finalized? :sold?]
                      :or {offset 0 limit -1 root-name "eth"}}]
  (let [sql-map (cond-> {:select fields
                         :from [:offerings]
                         :where unregistered-offerings-where-query
                         :offset offset
                         :limit limit}
                        original-owner (merge-where [:= :offering/original-owner original-owner])
                        new-owner (merge-where [:= :offering/new-owner new-owner])
                        min-price (merge-where [:>= :offering/price min-price])
                        max-price (merge-where [:<= :offering/price max-price])
                        min-length (merge-where [:>= :offering/label-length min-length])
                        max-length (merge-where [:<= :offering/label-length max-length])
                        (and min-end-time-now?
                             (not sold?)) (merge-where [:or
                                                        [:>= :auction-offering/end-time (to-epoch (t/now))]
                                                        [:= :auction-offering/end-time nil]])
                        sold? (merge-where [:and
                                            [:<> :offering/new-owner nil]
                                            [:<> :offering/new-owner emergency-state-new-owner]])
                        bidder (merge-left-join [:offering/bids :b] [:= :b.bid/offering :offerings.offering/address])
                        bidder (merge-where [:= :b.bid/bidder bidder])
                        bidder (update-in [:modifiers] concat [:distinct])
                        winning-bidder (merge-where [:= :auction-offering/winning-bidder winning-bidder])
                        exclude-winning-bidder (merge-where [:<> :auction-offering/winning-bidder exclude-winning-bidder])
                        version (merge-where [:= :offering/version version])
                        (false? finalized?) (merge-where [:= :offering/finalized-on 0])
                        (true? finalized?) (merge-where [:<> :offering/finalized-on 0])
                        (and (boolean? node-owner?) (not sold?)) (merge-where [:= :offering/node-owner? node-owner?])
                        (and buy-now? (not auction?)) (merge-where [:= :offering/buy-now? true])
                        (and auction? (not buy-now?)) (merge-where [:= :offering/auction? true])
                        (and top-level-names? (not sub-level-names?)) (merge-where [:= :offering/top-level-name? true])
                        (and sub-level-names? (not top-level-names?)) (merge-where [:= :offering/top-level-name? false])
                        exclude-special-chars? (merge-where [:= :offering/contains-special-char? false])
                        exclude-numbers? (merge-where [:= :offering/contains-number? false])
                        exclude-node (merge-where [:<> :offering/node exclude-node])
                        node (merge-where [:= :offering/node node])
                        nodes (merge-where [:in :offering/node nodes])
                        name (merge-where [:like :offering/name (str (name-pattern name name-position) "." root-name)])
                        order-by (merge-order-by (prepare-order-by order-by order-by-dir {:name name :root-name root-name})))]

    (merge
      {:items (db/all sql-map)}
      (when total-count?
        {:total-count (db/total-count sql-map)}))))
