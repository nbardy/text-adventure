(ns text-ad.unit
  (:require [text-ad.hook :refer-macros [defn-hookable]]))

(defn set-race [state race]
  (assoc-in state [:stats :race] race))

(defn get-race [entity]
  (get-in entity [:stats :race] :human))

(defn get-name [entity]
  (get-in entity [:name] "?"))

(defn copy-key [coll k1 k2]
  (assoc coll k2 (get coll k1)))

(defn-hookable available-actions [entity] {}) 
(defn-hookable get-modifiers [entity] [])

(defn-hookable compute-values [modifiers]
  "Accepts a list of modifiers of the form [type stat value]
  type: #{:add :mult}
  stat: Any key.
  value: Floats."
  (let [{:keys [mult add]} (group-by first modifiers)
        added (reduce (fn t [stats [_ stat v]] 
                        (update-in stats [stat] #(+ % v) :default 0)) {} add)
        multiplied ; Multiply second
        (reduce (fn [stats [_ stat v]] 
                  (update-in stats [stat] #(* % v) :default 0)) 
                added mult)] 
     multiplied))

(defn get-health [entity]
  (let [stats (compute-values (get-modifiers entity))]
    (* (:con stats) 5)))

(defn get-speed [entity]
  (let [stats (compute-values (get-modifiers entity))]
    (* (:dex stats) 20)))

(defn fight-stats [entity]
  (print :fs (dissoc entity :map))
  (-> (compute-values (get-modifiers entity))
      (assoc :actions (available-actions entity)
             :health (get-health entity)
             :max-health (get-health entity)
             :speed (get-speed entity)
             :name (get-name entity))))
