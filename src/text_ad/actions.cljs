(ns text-ad.actions
  (:require-macros [text-ad.macros :refer [defaction]])
  (:require [text-ad.util :refer [update-in]]
            [text-ad.messages]))


(defaction "Rub Eyes" :in all
  ([state] (and (not (get-in state [:got-up?]))
                (get-in state [:has-begun])))
  ([state] (update-in state [:eye-rubs] inc :default 0))
  ([state] (if (= 1 (get state :eye-rubs))
             "Vision clears. Still sitting. You see a mirror nearby."
             "Eye are already clear. Rubbing them just hurts")))

(defaction "Get Up" :in all
  ([state] (and (not (get-in state [:got-up?]))
                (get-in state [:eye-rubs])))
  ([state] (assoc state :got-up? true))
  ([state] ["You stand up, look around."
            "You have no idea where you are"]))

(defaction "Look Around" :in all
  ([state] (get-in state [:eye-rubs]))
  ([state] (assoc state :looking-around? true)))

(defn available? ^:export [state]
  "Return all available actions for a given state"
  (into {} (filter #((:available? (second %)) state) @all)))
