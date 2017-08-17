(ns district0x.ui.components.text-field
  (:require
    [district0x.shared.utils :as d0x-shared-utils :refer [http-url?]]
    [district0x.ui.utils :as d0x-ui-utils :refer [valid-length?]]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.impl.template :as tmpl]))

(def text-field*
  (tmpl/adapt-react-class
    (aget js/MaterialUI "TextField")
    ;; Optional...
    {:synthetic-input
     ;; A valid map value for `synthetic-input` does two things:
     ;; 1) It implicitly marks this component class as an input type so that interactive
     ;;    updates will work without cursor jumping.
     ;; 2) Reagent defers to its functions when it goes to set a value for the input component,
     ;;    or signal a change, providing enough data for us to decide which DOM node is our input
     ;;    node to target and continue processing with that (or any arbitrary behaviour...); and
     ;;    to handle onChange events arbitrarily.
     ;;
     ;;    Note: We can also use an extra hook `on-write` to execute more custom behaviour
     ;;    when Reagent actually writes a new value to the input node, from within `on-update`.
     ;;
     ;;    Note: Both functions receive a `next` argument which represents the next fn to
     ;;    execute in Reagent's processing chain.
     {:on-update (fn [next root-node rendered-value dom-value component]
                   (let [input-node (.querySelector root-node "input")
                         textarea-nodes (array-seq (.querySelectorAll root-node "textarea"))
                         textarea-node (when (= 2 (count textarea-nodes))
                                         ;; We are dealing with EnhancedTextarea (i.e.
                                         ;; multi-line TextField)
                                         ;; so our target node is the second <textarea>...
                                         (second textarea-nodes))
                         target-node (or input-node textarea-node)]
                     (when target-node
                       ;; Call Reagent's input node value setter fn (extracted from input-set-value)
                       ;; which handles updating of a given <input> element,
                       ;; now that we have targeted the correct <input> within our component...
                       (next target-node rendered-value dom-value component
                             ;; Also hook into the actual value-writing step,
                             ;; since `input-node-set-value doesn't necessarily update values
                             ;; (i.e. not dirty).
                             {:on-write
                              (fn [new-value]
                                ;; `blank?` is effectively the same conditional as Material-UI uses
                                ;; to update its `hasValue` and `isClean` properties, which are
                                ;; required for correct rendering of hint text etc.
                                (if (clojure.string/blank? new-value)
                                  (.setState component #js {:hasValue false :isClean false})
                                  (.setState component #js {:hasValue true :isClean false})))}))))
      :on-change (fn [next event]
                   ;; All we do here is continue processing but with the event target value
                   ;; extracted into a second argument, to match Material-UI's existing API.
                   (next event (-> event .-target .-value)))}}))

(defn text-field [{:keys [:value] :as props}]
  [text-field*
   (r/merge-props
     {:style {:display :block}
      :floating-label-fixed (boolean (seq (str value)))}
     props)])

(defn text-field-with-length []
  (fn [{:keys [:value :on-change :max-length :min-length :max-length-error-text
               :min-length-error-text] :as props}]
    [text-field
     (r/merge-props
       {:error-text (when-not (valid-length? value max-length min-length)
                      (if (pos? min-length)
                        (or min-length-error-text
                            (gstring/format "Write between %s and %s characters" min-length max-length))
                        (or max-length-error-text
                            "Text is too long")))
        :on-change (fn [e value]
                     (when (fn? on-change)
                       (on-change e value (valid-length? value max-length min-length))))}
       (dissoc props :min-length :max-length :max-length-error-text :min-length-error-text))]))

(defn url-field []
  (fn [{:keys [:value :on-change :max-length :allow-empty? :max-length-error-text :url-error-text]
        :as props}]
    [text-field
     (r/merge-props
       {:error-text (if-not (valid-length? value max-length)
                      (or max-length-error-text
                          (gstring/format "URL must be shorter than %s characters" max-length))
                      (when-not (http-url? value {:allow-empty? allow-empty?})
                        (or url-error-text
                            "Invalid URL")))
        :on-change (fn [e value]
                     (when (fn? on-change)
                       (on-change e value (and (valid-length? value max-length)
                                               (http-url? value {:allow-empty? allow-empty?})))))}
       (dissoc props :allow-empty? :max-length :max-length-error-text :url-error-text))]))

(defn ether-field [{:keys [:value :on-change :on-change :allow-empty?
                           :value-error-text :only-positive?] :as props}]
  (let [validator (if only-positive? d0x-shared-utils/pos-ether-value? d0x-shared-utils/non-neg-ether-value?)]
    [text-field
     (r/merge-props
       {:on-change (fn [e value]
                     (if on-change
                       (on-change e value (validator value (select-keys props [:allow-empty?])))))
        :error-text (when-not (validator value (select-keys props [:allow-empty?]))
                      (or value-error-text "Invalid value"))}
       (dissoc props :on-change :allow-empty? :only-positive? :value-error-text))]))

