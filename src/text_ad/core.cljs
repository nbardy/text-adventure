(ns text-ad.core
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [text-ad.util :refer [create-element!]]
            [clojure.browser.repl :as repl]
            [text-ad.render.core :as render]
            [text-ad.state :as state]
            [text-ad.map :as map]
            [text-ad.actions :as actions]
            [om.core :as om :include-macros true]
            [figwheel.client :as fw]))

(enable-console-print!)


(def rows 40)
(def cols 40)
(def seed 3)

(map/set-seed! seed)
(defonce init-state (atom {:map (map/create) 
                           :row 150 :col 150
                           :messages []
                           :zoom 2}))

(repl/connect "http://localhost:9000/repl")
(do-in 2000
  (swap! init-state update-in [:messages] conj "Confused, you can't seem to remember much of anything."))


(do-in 5000
  (swap! init-state update-in [:messages] conj "What happened?"))

(do-in 7000
  (swap! init-state update-in [:messages] conj "Laying on your back, vision is foggy, head throbbing...")
  (swap! init-state assoc :mode :start))


(om/root render/app-view init-state {:target js/document.body})

(aset js/document "onkeydown" 
      (fn [e] (case js/window.event.keyCode
                39 (swap! init-state state/move :right)
                40 (swap! init-state state/move :down)
                38 (swap! init-state state/move :up)
                37 (swap! init-state state/move :left))))

(fw/watch-and-reload
 :jsload-callback (fn [] 
                    (om/root render/app-view init-state {:target js/document.body})
                    (print "refresh")))
