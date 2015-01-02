(ns text-ad.battle
  (:refer-clojure :exclude [update-in])
  (:require [text-ad.util :refer [update-in get-entity]]
            [text-ad.hook :refer-macros [defn-hookable]]))

(declare defense
         attack
         health)


(defn alive? [unit]
  (> (:health unit) 0))


(let [memed 
      (memoize 
        (fn [all]
          (->> (range) (drop 1)
               ; Cycle turns based on speed
               (map (fn [tick] (filter #(= 0 (mod tick (% :speed))) all)))
               (remove empty?) (flatten) (map :id))))]
  (defn turns [entities]
    ; This function only relies on speed and id so we can memoize on 
    ; these two keys to drastically speed up.
    (memed (map #(select-keys % [:id :speed]) entities))))

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
