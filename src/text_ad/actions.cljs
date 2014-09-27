(ns text-ad.actions
  (:refer-clojure :exclude [update-in])
  (:require-macros [text-ad.macros :refer [defaction defdialogue]])
  (:require [text-ad.util :refer [update-in]]
            [text-ad.state :refer [nearby-items set-race]]))

(def blind-num 5)
(defn update-eye-rub [state]
  (-> (update-in state [:eye-rubs] inc :default 0)
      (#(if (= (get-in % [:eye-rubs] 0) blind-num)
          (assoc-in % [:stats :eyesight] :blind)
          %))))

(defn mode-is? [state v]
  (= (get state :mode) v))

(defaction "Rub Eyes" :in all
  ([state] (and (mode-is? state :start)
                (< (get-in state [:eye-rubs] 0) blind-num)))
  ([state] (update-eye-rub state))
  ([state] (condp = (get state :eye-rubs)
             blind-num "You can no longer see anything."
             (- blind-num 1)
             ["It's starting to really hurt. I should stop."
              "Don't want to go blind"]
             1 ["Vision clears. Still sitting."
                "Need to figure out who I am. What I do..." ]
             "Eye are already clear. Rubbing them just hurts")))


(defaction "Get Up" :in all
  ([state] (and (mode-is? state :start)
                (get-in state [:eye-rubs])
                (get-in state [:looked-around?])))
  ([state] (assoc state :mode :walking))
  ([state] ["You stand up, look around."
            "You have no idea where you are."]))

(defaction "Look Around" :in all
  ([state] (and (mode-is? state :start)
                (get-in state [:eye-rubs])
                (not (get-in state [:looked-around]))))
  ([state] (assoc state :looked-around? true))
  ([state] "You see a broken piece of mirror near-by."))

(defdialogue "Check Mirror" :in all
  :description "Looking in the mirror you see a face starers back at you. You recognize it as the face of a ..."
  ([state] (and (mode-is? state :start)
                (get state :looked-around?)
                (not (get-in state [:stats :race]))))
  "Human" ([state] true) ([state] (set-race state :human))
  "Dwarf" ([state] false) ([state] (set-race state :dwarf))
  "Elf" ([state] (set-race state :elf))
  "Orc" ([state] (set-race state :orc)))

(defn available? ^:export [state]
  "Return all available actions for a given state"
  (into {} (filter #((:available? (second %)) state) @all)))
