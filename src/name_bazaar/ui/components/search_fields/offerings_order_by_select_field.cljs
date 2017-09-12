(ns name-bazaar.ui.components.search-fields.offerings-order-by-select-field
  (:require
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(def offerings-order-by-options
  {:offering.order-by/newest [[:created-on :desc] "Newest"]
   :offering.order-by/most-active [[:bid-count :desc] "Most Active"]
   :offering.order-by/most-expensive [[:price :desc] "Most Expensive"]
   :offering.order-by/cheapest [[:price :asc] "Cheapest"]
   :offering.order-by/ending-soon [[:end-time :asc] "Ending Soon"]
   :offering.order-by/finalized-newest [[:finalized-on :desc] "Newest"]
   :offering.order-by/finalized-oldest [[:finalized-on :asc] "Oldest"]})

(defn offerings-order-by-select-field [{:keys [:on-change :options :order-by-column :order-by-dir] :as props}]
  (let [options (if options (select-keys offerings-order-by-options options) offerings-order-by-options)
        options-vals (vals options)]
    [ui/select-field
     (r/merge-props
       {:hint-text "Order By"
        :value (str [order-by-column order-by-dir])}
       (merge
         (dissoc props :options :order-by-column :order-by-dir)
         {:on-change (fn [e index]
                       (let [[order-by-column order-by-dir] (first (nth options-vals index))]
                         (on-change order-by-column order-by-dir)))}))
     (for [[val text] options-vals]
       [ui/menu-item
        {:key (str val)                                     ; hack, because material-ui selectfield
         :value (str val)                                   ; doesn't support non-primitive values
         :primary-text text}])]))
