(ns text-ad.vectors)

(def add (partial merge-with +))

(defn scale [fac vec] 
  "Scales a vector of form {:key val} to {:key fac * val} for all keys."
  (into {} (map (fn [[k v]] [k (* fac v)]) vec)))

(defn sub [vec1 vec2] (add vec1 (scale -1 vec2)))

(defn mag [vec]
  "Returns the magnitude of a vector"
  (js/Math.sqrt (reduce (fn [m [k v]] (+ m (* v v))) 0 vec)))

(defn dist [vec1 vec2]
  (mag (sub vec1 vec2)))

(defn unit [vec]
  "Returns the unit-vector of the given vector."
  (if (-> vec vals first (= 0))
    vec
    (scale (/ (mag vec)) vec)))

(defn orthogonal [vec]
  "Returns a vector orthogonal to the current.
  NOTE: Currently only works on two dimensional vectors."
  (if (-> vec count (= 2))
    (let [[k1 k2] (keys vec)
          [v1 v2] (vals vec)]
      [{k1 (- v2) k2 v1}
       {k1 v2 k2 (- v1)}])
    (throw (js/Error. "Orthogonal only accepts vectors of dimension 2."))))
  
(def epsilon 0.000001)
(defmulti =? 
  "Defines mathematical equality for numbers and collections of numbers
  with a tolerance of epilson."
  (fn [subject & others] 
    (if (-> others count (> 1)) 
      :multiple ;Dispatch to multiple if passed more than two args
      (type subject))))

(defmethod =? :multiple [subject & others] (every? #(=? subject %) others))

(defmethod =? cljs.core/PersistentVector [subject others]
  (every? true? (map #(=? % %2) subject others)))

(defmethod =? cljs.core/PersistentArrayMap
  ([subject other] (every? (fn [[k v]] (=? v (k other))) subject)))

(defmethod =? :default 
  ([subject other] (and (< (- subject epsilon) other)
                        (> (+ subject epsilon) other))))
