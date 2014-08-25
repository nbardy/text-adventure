(ns text-ad.util)

(defn in-between? [value [p1 p2]]
  (or (< p1 value p2) 
      (> p1 value p2)))

(defn create-element! [ele-name & attrs]
  (let [ele (.createElement js/document ele-name)]
    (doseq [[k v] (first attrs)]
      (.setAttribute ele (name k) v))
    ele))

(defn current-time$ 
  "Returns the current time.
   NOTE: Non-pure function."
  [] (.now js/Date))
