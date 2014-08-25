(ns text-ad.map
  (:require-macros [text-ad.macros :refer [defmap]]))

(defn grass [] :grass )
(defn mountain [] :mountain )
(defn river [] :river )
(defn hills [] :hills )
(defn plains [] :plains )
(defn water-temple [] :water-temple )

(defonce seed (atom nil))
(defn set-seed! ^:export [new-seed]
  (reset! seed new-seed))

(def p2 (* 12926335 1373500))
(def p1 (* 119263423 11735001))

(defn seeded-rand 
  ([high] 
   (swap! seed #(-> % (* p2) (mod p1)))
   (-> @seed (/ p1) (* high) (int)))
  ([low high] 
   (+ low (seeded-rand (- high low)))))

(defn wrap [[row col] grid] 
  [(mod row (count grid)) (mod col (count (first grid)))])

(def config 
  {:rows 100
   :cols 100
   :mountains {:count 5
               :segments {:amount [4 20]
                          :length [2 4]}}})

(def directions [[0 1] [1 0] [-1 0] [0 -1] 
                      [1 1] [-1 -1] [1 -1] [-1 1]])

(def neighbor-shifts [[0 1] [1 0] [0 -1] [-1 0]])

(def add (partial map +))

(defn rand-line [start length]
  (let [direction (directions (seeded-rand (count directions)))]
    (take (apply seeded-rand length)
          (iterate (partial add direction) start))))

(defn rand-jagged-line [start {:keys [amount length]}] 
  (nth (iterate #(concat % (rand-line (last %) length )) [start])
       (apply seeded-rand amount)))

(defn neighbors [spot]
  (map #(add spot %) neighbor-shifts))

(defn cells-to [grid cells f]
  (reduce #(assoc-in % %2 (f)) 
          grid 
          (filter #(get-in grid %) cells)))

(defn add-mountain-range [grid]
  (let [rows (count grid)
        cols (-> grid first count)
        start [(seeded-rand -5 (+ -5 rows))
               (seeded-rand -5 (+ -5 cols))]
        segments (get-in config [:mountains :segments])
        core (map #(wrap % grid) 
                  (rand-jagged-line start segments))
        fringe (partition 2 (flatten (map neighbors core)))]
    (-> grid
        (cells-to fringe hills)
        (cells-to core mountain))))

(defn add-mountains [grid]
  (let [n (get-in config [:mountains :count])]
    (nth (iterate #(add-mountain-range %) grid) 
         n)))

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
   R river
   P plains
   D (rand-terrain 80 hills 100 mountain)
   T water-temple]
  D D M R M D D
  D D M M R M D
  D D M M M R M
  D D M M R M M
  D M M R M M D
  D M R M M D D
  D M R M D D D
  D M R M M D D
  D M M R M M D
  D D M M R M D
  D D M R M M M
  D M M R M M D
  D M R R R M M
  M M R R R M M
  M R R R R R M
  M M P P P M M
  D M M T M M D
  D D M M M D D)

(def wr water-temple-map)

(def col-count (memoize (fn [grid] (count (first grid)))))
(def col-count (memoize (fn [grid] (count grid))))

(defn add-nums [grid]
  (vectorify
    (for [[row r] (map list grid (range))]
      (for [[cell c] (map list row (range))] [cell r c]))))

(defn create [] 
  (let [{:keys [rows cols]} config
        grid (vectorify (for [row (range rows)]
               (for [col (range cols)] (grass)))) ]
    (-> grid
        (add-mountains))))
