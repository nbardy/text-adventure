(ns text-ad.state
  (:refer-clojure :exclude [update-in])
  (:require [text-ad.unit :as unit]
            [text-ad.util :refer [update-in mode-is?]]
            [text-ad.hook :refer-macros [defn-hookable]]))

(defn nearby-items [{:keys [row col grid] :as state}]
  [:mirror])

(def enemy-count 5)

(let [id (atom 0)]
  (defn next-id! [] (swap! id inc)))
      
(defn-hookable available-monster-fns [state] [])

(defn rand-enemy [state]
  ((rand-nth (available-monster-fns state)) state))

; Wrapper function which accpets a function that returns a hash
; and wraps that function 
(defn add-id [m]
  (assoc m :id (next-id!)))

(defn rand-fight [state]
  (if (> (:moves-since-fight state) 20)
    (do (print :allies (:allies state))
    (-> state
        (assoc :fight 
               {:turn 0
                :enemies (vec 
                           (repeatedly (rand enemy-count)
                                       #(add-id (unit/fight-stats (rand-enemy state)))))
                :allies (vec (cons (add-id (unit/fight-stats state))
                                   (map #(add-id (unit/fight-stats %))
                                        (state :allies))))})
        (assoc :mode :fighting)
        (assoc :moves-since-fight 0)))
    state))

(defn move [state dir]
  (let [[k f] (case dir
                :left  [[:col] dec]
                :right [[:col] inc]
                :down  [[:row] inc]
                :up    [[:row] dec])]
    ;; (print :row (state :row) :col (state :col))
    ;; (print (get-in (:map state) ((juxt :row :col) (update-in state k f) )))
    (-> (if (mode-is? state :walking)
          (update-in state k f) state)
        (update-in [:moves] inc :default 0)
        (update-in [:moves-since-fight] inc :default 0)
        (rand-fight))))
