(ns text-ad.battle
  (:require-macros [text-ad.macros :refer [battle-action]]))

(defn dmg [n]
  (battle-action [fight-state target-key] 
    (update-in fight-state (conj target-key :health) #(- % n))))

(declare defense
         attack
         health)

(defn fight-stats [state]
  {:attack (attack state)
   :defense (defense state)
   :health (health state)})

(defn attack [state] 5)
(defn defense [state] 7)
(defn health [state] 12)

(defn available-actions [state]
  {:punch (dmg 5)})
