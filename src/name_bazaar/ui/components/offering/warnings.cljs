(ns name-bazaar.ui.components.offering.warnings
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.string :as string]
    [name-bazaar.shared.utils :refer [name-label]]
    [name-bazaar.ui.styles :as styles]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn non-ascii-characters-warning [props]
  [:div
   (r/merge-props
     {:style styles/warning-color}
     (dissoc props))
   "WARNING: This name contains non-latin characters. Some of these characters may look same or "
   "very similar to their standard latin counterparts. If you're unsure what this means, please contact "
   "our team."])

(defn missing-ownership-warning [props]
  [:div
   (r/merge-props
     {:style styles/warning-color}
     props)
   "WARNING: Original owner removed or haven't transferred ownership of this name from offering contract. "
   "For this reason buying or bidding for this name is not possible."])

(defn sub-level-name-warning [{:keys [:offering/name] :as props}]
  (let [parent-name (string/replace name (name-label name) "")]
    [:div
     (r/merge-props
       {:style styles/warning-color}
       props)
     "WARNING: This is not top level name. Beware, that owner of " parent-name " will be always able to take this "
     "name back. Buy only when you trust a owner of " parent-name "."]))