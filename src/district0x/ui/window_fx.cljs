(ns district0x.ui.window-fx
  (:require
    [cljs.spec.alpha :as s]
    [goog.events :as events]
    [re-frame.core :as re-frame :refer [reg-fx console reg-event-db reg-event-fx]]))

(s/def ::dispatch (s/coll-of keyword))
(s/def ::resize-interval int?)
(s/def ::on-resize-args (s/keys :req-un [::dispatch ::resize-interval]))

(defn on-resize [{:keys [:dispatch :resize-interval]} timer]
  (js/clearTimeout @timer)
  (reset! timer (js/setTimeout #(re-frame/dispatch (into [] (concat dispatch [js/window.innerWidth js/window.innerHeight])))
                               resize-interval)))

(reg-fx
  :window/on-resize
  (fn [config]
    (s/assert ::on-resize-args config)
    (let [timer (atom nil)]
      (events/listen js/window events/EventType.RESIZE (partial on-resize config timer)))))

(reg-fx
  :window/on-focus
  (fn [{:keys [:dispatch]}]
    (events/listen js/window events/EventType.FOCUS #(re-frame/dispatch dispatch))))

(reg-fx
  :window/on-blur
  (fn [{:keys [:dispatch]}]
    (events/listen js/window events/EventType.BLUR #(re-frame/dispatch dispatch))))

(reg-fx
  :window/scroll-to-top
  (fn [_]
    (.scrollTo js/window 0 0)))
