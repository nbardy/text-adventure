(ns text-ad.core
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [text-ad.util :refer [create-element!]]
            [clojure.browser.repl :as repl]
            [text-ad.render.core :as render]
            [text-ad.state :as state]
            [text-ad.map :as map]
            [text-ad.actions :as actions]
            [om-devtools.core :as dev-tools]
            [om.core :as om :include-macros true]
            [figwheel.client :as fw]))

(enable-console-print!)

(def rows 40)
(def cols 40)
(def seed 3)

(defn load-extensions! [extensions]
  (doseq [ext extensions] (js/goog.require ext)))

; Load extensions
(def core-extensions
  ["text_ad.extensions.items"
   "text_ad.extensions.race"
   "text_ad.extensions.monsters"])

(map/set-seed! seed)

(defonce init-state (atom {:map (map/create) 
                           :row 150 :col 150
                           :allies 
                           [{:name "Juan"
                             :stats {:race :elf}}]
                           :messages []
                           :zoom 6}))

;; (add-watch init-state :printer
;;            (fn [_ _ old new] (print (dissoc new :map))))

(defonce initialized? 
  (do (repl/connect "http://localhost:9000/repl")
      (load-extensions! core-extensions)
      (do-in 2000 (swap! init-state update-in [:messages] conj 
                         "Confused, you can't seem to remember much of anything."))
      (do-in 5000 (swap! init-state update-in [:messages] conj "What happened?"))
      (do-in 7000 (swap! init-state update-in [:messages] conj 
                         "Laying on your back, vision is foggy, head throbbing...")
             (swap! init-state assoc :mode :start))))


(defn run []
  (dev-tools/root render/app-view init-state 
    {:target js/document.body
     :hide-keys [:map]}))
(run)


(aset js/document "onkeydown" 
      (fn [e]  
        (case (or (and js/window.event js/window.event.keyCode) (.-keyCode e))
          39 (swap! init-state state/move :right)
          40 (swap! init-state state/move :down)
          38 (swap! init-state state/move :up)
          37 (swap! init-state state/move :left))))


(fw/watch-and-reload
 :jsload-callback (fn [] 
                    ;; (map/set-seed! seed)
                    ;; (swap! init-state assoc :map (map/create))
                    (run)
                    (load-extensions! core-extensions)
                    (print "refresh")))
