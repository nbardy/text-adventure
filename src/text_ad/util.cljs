(ns text-ad.util)

(defn n-times ([f x n] (if (> n 0) (recur f (f x) (dec n)) x))
  ([f n] (n-times f (f) (dec n))))

(defn update-in [m ks f & args]
  "Wrap clojure.core.update-in to accept a optional last argument of 
  :default default-value to passed to the update function if key
  is not-found."
  ; Set the default value in map if default is provided and 
  ; the key does not exist. 
  ; Check on ::not-found to allow for existent nil values.
  (let [has-default (and args (= (last (butlast args)) :default))
        m (if (and has-default (= ::not-found (get-in m ks ::not-found)))
            (assoc-in m ks (last args)) m)
        args (if has-default (drop-last 2 args) args)]
    (apply clojure.core/update-in m ks f args)))

(defn in-between? [value [p1 p2]]
  (or (< p1 value p2) 
      (> p1 value p2)))

(defn create-element! [ele-name & attrs]
  (let [ele (.createElement js/document ele-name)]
    (doseq [[k v] (first attrs)]
      (.setAttribute ele (name k) v))
    ele))

(defn current-time
  "Returns the current time.
   NOTE: Non-pure function."
  [] (.now js/Date))

(defn with-rev-index [x] (reverse (map list (reverse x) (range))))

(defn deg->uvec [deg] [(js/Math.cos deg) (js/Math.sin deg)])

(defn flatten-1 [coll]  
  (mapcat  #(if (sequential? %) % [%]) coll))

(def css-trans-group (-> js/React (aget "addons") 
                         (aget "CSSTransitionGroup"))) 
