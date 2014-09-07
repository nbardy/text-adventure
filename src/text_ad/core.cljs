(ns text-ad.core
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [text-ad.util :refer [create-element! current-time$]]
            [text-ad.render.core :as render]
            [text-ad.map :as map]
            [text-ad.game :refer [advance]]
            [text-ad.actions :as actions]
            [text-ad.messages :as messages]
            [om.core :as om :include-macros true]
            [figwheel.client :as fw]))

(enable-console-print!)


(def rows 40)
(def cols 40)
(def seed 3)

(map/set-seed! seed)
(defonce init-state (atom {:map (map/create) 
                           :row 11 :col 11
                           :messages []
                           :zoom 20}))

(do-in 2000
  (swap! init-state update-in [:messages] conj "Confused, you can't seem to remember much of anything."))


(do-in 5000
  (swap! init-state update-in [:messages] conj "What happened?"))

(do-in 7000
  (swap! init-state update-in [:messages] conj "Laying on your back, vision is foggy, head throbbing...")
  (swap! init-state assoc :has-begun true))


(om/root render/app-view init-state {:target js/document.body})

(aset js/document "onkeydown" 
      (fn [e] (case js/window.event.keyCode
                39 (swap! init-state update-in [:col] inc)
                37 (swap! init-state update-in [:col] dec)
                38 (swap! init-state update-in [:row] dec)
                40 (swap! init-state update-in [:row] inc))))

(defn with-timestamp [state]
  (assoc state [:timestamp] (current-time$)))

(fw/watch-and-reload
 :jsload-callback (fn [] 
                    (om/root render/app-view init-state {:target js/document.body})
                    (print "refresh")))
