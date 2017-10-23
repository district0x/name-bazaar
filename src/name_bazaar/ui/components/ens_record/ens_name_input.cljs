(ns name-bazaar.ui.components.ens-record.ens-name-input
  (:require
    [clojure.string :as string]
    [district0x.ui.components.input :refer [input]]
    [name-bazaar.ui.constants :as constants]
    [reagent.core :as r]
    [soda-ash.core :as ui]))


(defn ens-name-input [{:keys [:value :on-change] :as props}]
  [input
   (r/merge-props
     {:action (r/as-element [ui/Label constants/registrar-root])}
     (merge
       props
       {:on-change (fn [e data]
                     (let [new-val (aget data "value")]
                       (when (and (< 1 (- (count new-val) (count value))) ;; Detect paste
                                  (string/ends-with? new-val constants/registrar-root))
                         (aset data "value" (.slice new-val 0 (* (count constants/registrar-root) -1))))
                       (on-change e data)))}))])
