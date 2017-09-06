(ns name-bazaar.ui.subs.infinite-list-subs
  (:require
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :infinite-list
  (fn [db]
    (:infinite-list db)))

(reg-sub
  :infinite-list/expanded-items
  :<- [:infinite-list]
  (fn [infinite-list]
    (:expanded-items infinite-list)))

(reg-sub
  :infinite-list/items-heights
  :<- [:infinite-list/expanded-items]
  (fn [expanded-items [_ items-count default-height]]
    (map #(get-in expanded-items [% :height] default-height) (range items-count))))

(reg-sub
  :infinite-list.item/expanded?
  :<- [:infinite-list/expanded-items]
  (fn [expanded-items [_ index]]
    (boolean (get expanded-items index))))

(reg-sub
  :infinite-list.item/expanded-body-height
  :<- [:infinite-list/expanded-items]
  (fn [expanded-items [_ index collapsed-height]]
    (if-let [item-height (get-in expanded-items [index :height])]
      (- item-height collapsed-height)
      0)))


