(ns text-ad.core
  (:require [text-ad.util :refer [create-element! current-time$]]
            [text-ad.graphics :as graphics]
            [text-ad.map :as map]
            [text-ad.game :refer [advance]]
            [om.core :as om :include-macros true]
            [figwheel.client :as fw]))

(enable-console-print!)

(def rows 40)
(def cols 40)
(def seed 3)

(map/set-seed! seed)
(defonce init-state (atom {:map (map/create) 
                           :row 11 :col 11
                           :zoom 5}))
(om/root graphics/game init-state {:target js/document.body})

(aset js/document "onkeydown" 
      (fn [e] (case js/window.event.keyCode
                39 (swap! init-state update-in [:col] inc)
                37 (swap! init-state update-in [:col] dec)
                38 (swap! init-state update-in [:row] dec)
                40 (swap! init-state update-in [:row] inc))))

(defn with-timestamp [state]
  (assoc state [:timestamp] (current-time$)))

; TODO: Remove. This is for testing
;(defn advance [state]
  ;(map/set-seed! seed)
  ;{:map (map/create)})
;
;(defn animate! [state canvas]
  ;(js/requestAnimationFrame (fn []
      ;(animate! (advance (with-timestamp state)) canvas)))
  ;(graphics/render! canvas state))
;
;(def init-state {:map (map/create )})
;
;(defn start-loop! [canvas]
  ;(animate! (with-timestamp init-state) canvas))

;(defn start! []
  ;(start-loop! (let [ele (create-element! "div" {:id "map"})]
                 ;(set! (.-width ele) js/innerWidth)
                 ;(set! (.-height ele) js/innerHeight)
                 ;(.appendChild js/document.body ele)
                 ;ele)))

;(when-not @started
  ;(print "Started...")
  ;(reset! started true)
  ;(start!))

(fw/watch-and-reload
 :jsload-callback (fn [] 
                    (reset! init-state @init-state)
                    (print "refresh")))
