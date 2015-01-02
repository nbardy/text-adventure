(ns text-ad.battle-actions)

(defn dmg [n & {:keys [cooldown]}]
  (let [cd-key (keyword (gensym "dmg-cooldown"))]
    {:available 
     (fn [fight-state] 
       (if cooldown (> (get fight-state :turn 0) 
                       (fight-state cd-key)) true))
     :effect
     (fn [fight-state target-key active] 
       (-> fight-state 
           (update-in (conj target-key :health) #(- % n))
           (assoc cd-key (+ cooldown (fight-state :turn)))))}))
