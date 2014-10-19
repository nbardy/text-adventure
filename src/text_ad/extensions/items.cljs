(ns text-ad.extensions.items
  (:require [text-ad.hook :refer [add-hook!]]))

(add-hook! text-ad.battle/compute-scores 
           (fn [f args] (print (f args)) (f args)))
