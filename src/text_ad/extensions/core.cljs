(ns text-ad.extensions.core)

(defn add-race! [race-key options]
  "Accepts a keyword for the value of the race and an options hash.
  Available options:
  :modifiers
  :actions"
  (let [options-map {:modifiers [concat text-ad.unit/get-modifiers]
                     :actions [merge text-ad.unit/available-actions]}]
    (doseq [[item-k item-val] options]
      (let [[combine orig-fn] (options-map item-k)]
        (print :adding-hook item-k)
        (text-ad.hook/add-hook! 
          orig-fn
          (fn [f & args]
            (print :activating-hook item-k)
            (print :first-args (dissoc (first args) :map))
            (let [race (text-ad.unit/get-race (first args))]
            (print :activating-hook item-k :checking race := race-key)
(when (= race race-key) (print :adding item-val))
              (combine (apply f args) 
                       (if (= race race-key) item-val))))))))
  race-key)

(def all-stats [:str :dex :con :wis :int :cha])

(defn new-race! [race-key & {:keys [actions] :as options}]
  "A macro to add a race to the game." 
  (let [stats (mapv #(vector :add % (or (get options %) 1)) all-stats)]
  (add-race! race-key {:modifiers stats
                       :actions actions})))

