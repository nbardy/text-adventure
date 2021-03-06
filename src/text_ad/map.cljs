(ns text-ad.map
  (:require-macros [text-ad.macros :refer [defmap]])
  (:require [text-ad.util :refer [flatten-1 n-times deg->uvec]]
            [text-ad.vectors :as v]))

(def config 
  {:rows 200
   :cols 200
   :mountain {:count [3 5]
              :f (fn [] :mountain)
              :segments {:deg-shift [-130 130]
                         :amount [5 7]
                         :length [8 12]}}
   :river {:count [5 7]
           :thickness [2 4]
           :segments {:deg-shift [-30 30]
                      :amount [25 47]
                      :length [3 7]}}
   :forest {:count [4 5]
            :f (fn [] :forest)
            :segments {:deg-shift [-230 230]
                       :amount [10 12]
                       :length [2 2]}}})

(defn grass [] :grass)
(defn mountain [] :mountain)
(defn hills [] :hills)
(defn plains [] :plains)
(defn water [] :water)
(defn forest [] :forest)
(defn water-temple [] :water-temple)

(defonce seed (atom nil))
(defn set-seed! ^:export [new-seed]
  (print "Seeding at: " new-seed)
  (reset! seed new-seed))

(def p2 (* 12926335 1373500))
(def p1 (* 119263423 11735001))

(defn wrap [[row col] grid] 
  [(mod row (count grid)) (mod col (count (first grid)))])

(defn wrap-and-round [grid] (comp #(map js/Math.round %) #(wrap % grid)))

(defn seeded-rand 
  ([high] 
   (swap! seed #(-> % (* p2) (mod p1)))
   (-> @seed (/ p1) (* high) (int)))
  ([low high] 
   (+ low (seeded-rand (- high low)))))



(def directions [[-1 -1] [0 -1] [1 -1] 
                 [-1 0]  [0 0]  [1 0]
                 [-1 1]  [0 -1] [1 1]])

(def neighbor-shifts [[0 1] [1 0] [0 -1] [-1 0]])

(def add (partial map +))

(defn line [start length direction]
  (take length (iterate (partial add direction) start)))

(defn rand-jagged-line [start {:as config :keys [amount length deg-shift]}] 
  ; Reduce with each new line starting at the tail of the last creating a coll.
  (let [init-deg (seeded-rand 0 js/Math.PI)]
    (reduce (fn [coll [line-length deg]] 
              (concat coll (rest (line (last coll) line-length (deg->uvec deg)))))
            [start]
            ; Reduce into a collection of degrees shifted randomly based on the previous degree.
            ; along with a random length to guide the line
            (reduce (fn [coll line-length] 
                      (let [new-deg (+ (last (last coll)) (* 0.01 (apply seeded-rand deg-shift)))]
                        (conj coll [line-length new-deg])))
                    [[(apply seeded-rand length) init-deg]]
                    (repeatedly (dec (apply seeded-rand amount)) 
                                #(apply seeded-rand length))))))

(declare terrain-line
         add-lines)

(defn neighbors [spot]
  (map #(add spot %) neighbor-shifts))

(defn cells-to [grid cells f]
  (reduce #(assoc-in % %2 (f)) 
          grid 
          (filter #(get-in grid %) cells)))

(declare add-river)

(defn add-rivers [grid]
  (let [n (apply seeded-rand (get-in config [:river :count]))]
    (n-times add-river grid n)))

(defn add-river [grid]
  (let [config (config :river)
        first-line (terrain-line grid (:segments config))
        direction (apply v/sub (take 2 (drop 1 first-line)))
        shift-vec (v/unit (first (v/orthogonal direction)))
        ls (apply concat
                  (map-indexed 
                    (fn [shift-scale line] 
                      (mapv #(v/add % (v/scale shift-scale shift-vec)) line))
                    (repeat (apply seeded-rand (config :thickness))
                            first-line)))]
    (cells-to grid (mapv #(map js/Math.round %) ls) water)))

(defn add-forests [grid]
  (add-lines grid (get-in config [:forest]) (fn [] :forest)))

(defn add-lines [grid config f]
  (let [n (apply seeded-rand (config :count))]
    (n-times #(cells-to % (terrain-line % (config :segments)) f) 
             grid n)))

(defn terrain-line [grid config]
  (let [rows (count grid)
        cols (-> grid first count)
        start [(seeded-rand -5 (+ -5 rows))
               (seeded-rand -5 (+ -5 cols))]]
    (map (wrap-and-round grid) (rand-jagged-line start config))))

(defn add-mountain-range [grid]
  (let [core  (terrain-line grid (get-in config [:mountain :segments]))
        fringe (flatten-1 (map neighbors core))]
    (-> grid
        (cells-to fringe hills)
        (cells-to core mountain))))

(defn add-mountains [grid]
  (let [n (apply seeded-rand (get-in config [:mountain :count]))]
    (n-times add-mountain-range grid n)))

(defn rand-terrain [& pairs]
  (fn [& more]
    (let [r (rand 100)]
      ((second 
         (first (filter #(< r (first %)) 
                        (partition 2 pairs))))))))

(defn vectorify [grid]
  (mapv vec grid))

(defn rotate 
  ([grid] (rotate grid 90))
  ([grid degree]
   (case degree
     270 (rotate (reverse grid))
     180 (reverse grid)
     90 (let [rows (count (first grid))
           cols (count grid)]
       (vectorify
         (partition cols
                    (for [row (range rows)
                          col (range cols)]
                      (get-in grid [col row] :a))))))))

(defmap water-temple-map
  :cols 7
  [M mountain
   W water
   P plains
   D (rand-terrain 80 hills 100 mountain)
   T water-temple]
  D D M W M D D
  D D M M W M D
  D D M M M W M
  D D M M W M M
  D M M W M M D
  D M W M M D D
  D M W M D D D
  D M W M M D D
  D M M W M M D
  D D M M W M D
  D D M W M M M
  D M M W M M D
  D M W W W M M
  M M W W W M M
  M W W W W W M
  M M P P P M M
  D M M T M M D
  D D M M M D D)

(def col-count (memoize (fn [grid] (count (first grid)))))
(def col-count (memoize (fn [grid] (count grid))))

(defn create [] 
  (print "seed" @seed)
  (let [{:keys [rows cols]} config
        grid (vectorify (for [row (range rows)]
               (for [col (range cols)] (grass)))) ]
    (-> grid
        (add-mountains)
        (add-rivers)
        (add-forests))))
