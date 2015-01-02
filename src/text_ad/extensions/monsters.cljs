(ns text-ad.extensions.monsters
  (:require [text-ad.hook :refer [add-hook!]]
            [text-ad.extensions.core :refer [new-race!]]
            [text-ad.battle-actions :refer [dmg]]
            [text-ad.unit :refer [get-race]]))

(defn always-available-goblin [f args]
  (conj (f args) (fn [state] {:stats {:race :goblin}
                              :items [:hammer]})))

(new-race! :goblin 
           :dex 800
           :con 10
           :actions {:scratch (dmg 1)})

(add-hook! text-ad.state/available-monster-fns always-available-goblin)
