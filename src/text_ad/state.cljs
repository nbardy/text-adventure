(ns text-ad.state
  (:refer-clojure :exclude [update-in])
  (:require [text-ad.battle :as battle :refer [available-actions]]
            [text-ad.util :refer [update-in mode-is?]]))

(defn nearby-items [{:keys [row col grid] :as state}]
  [:mirror])

(defn set-race [state race]
  (assoc-in state [:stats :race] race))

(def enemy-count 5)

(def next-id (atom 0))
(defn rand-enemy []
  {:health 20 :attack 2 :defense 2 :id (swap! next-id inc)})

(defn rand-fight [state]
  (if (> (:moves-since-fight state) 20)
    (-> state
        (assoc :fight 
               {:enemies (mapv #(rand-enemy) (range (rand enemy-count)))
                :actions (available-actions state)
                :allies (vec (cons (battle/fight-stats (state :stats))
                                   (map battle/fight-stats (state :allies))))})
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
