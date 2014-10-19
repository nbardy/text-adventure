(ns text-ad.extensions.race
  (:require [text-ad.hook :refer [add-hook!]]))

(defn default [f & args]
  (concat  [[:add :health 10] [:add :speed 400]] (apply f args)))

(add-hook! text-ad.battle/get-modifiers default)
