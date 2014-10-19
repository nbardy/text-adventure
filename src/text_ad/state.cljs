(ns text-ad.state
  (:refer-clojure :exclude [update-in])
  (:require [text-ad.battle :as battle :refer [dmg ]]
            [text-ad.util :refer [update-in mode-is?]]))

(defn nearby-items [{:keys [row col grid] :as state}]
  [:mirror])

(defn set-race [state race]
  (assoc-in state [:stats :race] race))

(def enemy-count 5)

(let [id (atom 0)]
  (defn next-id! [] (swap! id inc)))
      
(defn rand-enemy [state]
  {:race :goblin
   :actions {:scratch (dmg 1)}})

; Wrapper function which accpets a function that returns a hash
; and wraps that function 
(defn add-id [m]
  (assoc m :id (next-id!)))

(defn rand-fight [state]
  (if (> (:moves-since-fight state) 20)
    (-> state
        (assoc :fight 
               {:turn 0
                :enemies (vec 
                           (repeatedly (rand enemy-count)
                                       #(add-id (battle/fight-stats (rand-enemy state)))))
                :allies (vec (cons (add-id (battle/fight-stats state))
                                   (map #(add-id (battle/fight-stats %))
                                        (state :allies))))})
        (assoc :mode :fighting)
        (assoc :moves-since-fight 0))
    state))

(defn move [state dir]
  (let [[k f] (case dir
                :left  [[:col] dec]
                :right [[:col] inc]
                :down  [[:row] inc]
                :up    [[:row] dec])]
    (-> (if (mode-is? state :walking)
          (update-in state k f) state)
        (update-in k f)
        (update-in [:moves] inc :default 0)
        (update-in [:moves-since-fight] inc :default 0)
        (rand-fight))))
