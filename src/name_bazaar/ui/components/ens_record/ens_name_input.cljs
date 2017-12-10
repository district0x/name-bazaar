(ns name-bazaar.ui.components.ens-record.ens-name-input
  (:require
    [clojure.string :as string]
    [district0x.ui.components.input :refer [input]]
    [name-bazaar.ui.constants :as constants]
    [name-bazaar.ui.utils :refer [reverse-record-node namehash sha3 normalize strip-root-registrar-suffix valid-ens-name? path-for]]
    [name-bazaar.shared.utils :refer [top-level-name?]]
    [reagent.core :as r]
    [re-frame.core :refer [subscribe dispatch]]
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

(def ownership-status->text
  {:ens.ownership-status/empty-name ""
   :ens.ownership-status/loading "Checking ownership..."
   :ens.ownership-status/not-ens-record-owner "You are not owner of this name"
   :ens.ownership-status/not-deed-owner "You don't own this name's locked value"
   :ens.ownership-status/owner "You are owner of this name"})

(defn load-name-ownership [value]
  (let [full-name (str value constants/registrar-root)
        node (namehash full-name)]
    (dispatch [:ens.records/load [node] {:load-resolver? true}])
    (when (top-level-name? full-name)
      (dispatch [:registrar.entries/load [(sha3 value)]]))))

(defn ens-name-input-ownership-validated []
  (fn [{:keys [:on-change :value :warn-only?] :as props}]
    (let [full-name (when (seq value)
                      (str value constants/registrar-root))
          ownership-status @(subscribe [:ens.record/ownership-status full-name])
          error? (contains? #{:ens.ownership-status/not-ens-record-owner
                              :ens.ownership-status/not-deed-owner}
                            ownership-status)]
      [:div.input-state-label
       {:class (cond
                 error? (if warn-only?
                          :warning
                          :error)
                 (contains? #{:ens.ownership-status/owner} ownership-status) :success)}
       [ens-name-input
        (r/merge-props
         {:label "Name"
          :fluid true
          :value value
          :on-change (fn [e data]
                       (let [value (aget data "value")]
                         (when (valid-ens-name? value)
                           (let [value (normalize value)]
                             (aset data "value" value)
                             (on-change e data)
                             (load-name-ownership value)))))
          :error error?}
         (dissoc props :ownership-status :on-change :warn-only?))]
       [:div.ui.label (ownership-status->text ownership-status)]])))
