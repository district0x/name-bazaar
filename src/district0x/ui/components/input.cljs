(ns district0x.ui.components.input
  (:require
    [district0x.ui.utils :refer [parse-props-children valid-length?]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [soda-ash.core :as ui]))

(defn input []
  "Puts 'dirty' class if input is non-empty"
  "Puts 'focus' class if input is focused"
  "Prevents SemanticUI warning about value being null"
  (let [focus? (r/atom false)]
    (fn [{:keys [:value :on-focus :on-blur :value] :as props}]
      [ui/Input
       (r/merge-props
         {:class (str (when-not (empty? (str value)) "dirty")
                      (when @focus? " focus"))
          :on-focus (fn [& args]
                      (reset! focus? true)
                      (when (fn? on-focus)
                        (apply on-focus args)))
          :on-blur (fn [& args]
                     (reset! focus? false)
                     (when (fn? on-blur)
                       (apply on-blur args)))}
         (merge (dissoc props :value)
                {:value (if value value "")}))])))

(defn token-input [{:keys [:token-code] :as props :or {token-code "ETH"}}]
  [input
   (r/merge-props
     {:action (r/as-element [ui/Label token-code])}
     (dissoc props :token-code))])





