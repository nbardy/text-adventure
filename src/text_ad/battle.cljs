(ns text-ad.battle
  (:refer-clojure :exclude [update-in])
  (:require [text-ad.util :refer [update-in get-entity]]
            [text-ad.hook :refer-macros [defn-hookable]]))

(defn dmg [n]
  (fn [fight-state target-key active] 
    (update-in fight-state (conj target-key :health) #(- % n))))

(declare defense
         attack
         health)

(defn copy-key [coll k1 k2]
  (assoc coll k2 (get coll k1)))

(defn available-actions [entity]
  (case (get-in entity [:stats :race] :human)
    :goblin {:scratch (dmg 1)}
    {:punch (dmg 5)}))

(defn-hookable get-modifiers [entity] [])

(declare compute-scores)

(defn fight-stats [entity]
  (-> {:actions (available-actions entity)}
      (merge (compute-scores (get-modifiers entity)))
      (copy-key :health :max-health)))

(defn race-modifiers [entity]
  (case (get-in entity [:stats :race] :human)
    :orc    [[:mult :attack 1.2]
             [:add  :defense -5]
             [:mult  :dodge 0.8]
             [:add  :health 5]]
    :goblin [[:add :health 1]
             [:add :attack 1]]
    :gnome  [[:add :health 1]
             [:mult :dodge 1.2]
             [:mult :accuracy 1.2]]
    :elf    [[:mult :mpower 1.2]
             [:add :mpower 5]]
    :human  [[:add :health 5]]
    []))

(defn-hookable compute-scores [modifiers]
  "Accepts a list of modifiers of the form [type stat value]
  type: #{:add :mult}
  stat: Any key.
  value: Floats."
  (let [{:keys [mult add]} (group-by first modifiers)
        added (reduce (fn t [stats [_ stat v]] 
                        (update-in stats [stat] #(+ % v) :default 0)) {} add)]
        (reduce (fn [stats [_ stat v]] 
                  (update-in stats [stat] #(* % v) :default 0)) added mult)))

(defn alive? [unit]
  (> (:health unit) 0))

(defn turns [entities]
  ; This function only relies on speed and id so we can memoize on 
  ; these two keys to drastically speed up.
  ((memoize 
     (fn [all]
       (->> (range) (drop 1)
            ; Cycle turns based on speed
            (map (fn [tick] (filter #(= 0 (mod tick (% :speed))) all)))
            (remove empty?) (flatten) (map :id)))) 
   (map #(select-keys % [:id :speed]) entities)))

(defn next-turn [{:keys [turn allies enemies] :as fight-state}]
  (let [turns (turns (concat allies enemies))
        entity (get-entity (nth turns (inc turn)) 
                           (concat allies enemies))]
    ; Skip turn if not alive
    (if (alive? entity)
      (update-in fight-state [:turn] inc)
      (next-turn (update-in fight-state [:turn] inc)))))

(defn result [{:keys [enemies allies]}]
  (cond (not-any? alive? enemies) :victory
        (not-any? alive? allies) :loss
        :default nil))
