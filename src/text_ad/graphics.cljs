(ns text-ad.graphics
  (:require [om.core :as om :include-macros true]
            [text-ad.map :refer [vectorify] :as map]
            [text-ad.util :refer [in-between?]]
            [clojure.string :refer [join]]
            [sablono.core :refer-macros [html]]))

(defn trans [cell]
  (let [cell (if (keyword? cell) cell (keyword cell))]
    ({:grass "."
      :person "X"
      :mountain "A"  
      :river "~"
      :hills "^"
      :plains ","
      :water-temple "#"} cell)))

(defn trans-color ^:export [cell] 
  (let [ch (trans cell)]
    (str "rgb(" 
         (-> ch (.charCodeAt 0) (* 75684) (mod 255)) ","
         (-> ch (.charCodeAt 0) (* 94231) (mod 255)) ","
         (-> ch (.charCodeAt 0) (* 31349) (mod 255)) ")")))
                  

(defn px [x] (str x "px"))


(defn slice [grid [from-row to-row] [from-col to-col]]
  (vectorify
    (for [row (range from-row to-row)]
      (for [col (range from-col to-col)]
        (get-in grid (map/wrap [row col] grid))))))

(def events (atom []))

(defn +and- [x y] [(- x y) (+ x y)])
(declare map-component)

(defn +and-button [state owner {:keys [k]}]
  (om/component
    (html [:div
           [:span (str (name k) ": ")]
           [:span (state k)]
           [:button {:onMouseDown 
                     (fn [& update] 
                       (om/transact! state k inc))} "+"]
           [:button {:onMouseDown 
                     (fn [& update] 
                       (om/transact! state k dec))} "-"]])))


(defn game [state owner]
  (om/component
    (html [:div "Hello world!"
           (om/build +and-button state {:opts {:k :zoom}})
           (for [k [:row :col]]
             (om/build +and-button state {:opts {:k k}}))
           (om/build map-component 
                     [(assoc-in (state :map) 
                                (map/wrap [(state :row) (state :col)] 
                                          (state :map)) 
                                :person)
                      [(state :row) (state :col)] 
                      {:cell-height (state :zoom) 
                       :cell-width (state :zoom)}])])))

; TODO: Rewrite with a macro to use js for loops
(defn draw-grid [ctx [grid & [old-grid]] [cell-width cell-height]
                 & {:keys [redraw?]}]
  (doseq [row-num (range (count (first grid)))]
    (doseq [col-num (range (count grid))]
      ; Check last draw cell if available and not forcing redraw
      (when (or redraw?
                (not (= (get-in old-grid [row-num col-num]) 
                        (get-in grid [row-num col-num]))))
        (aset ctx "fillStyle" (trans-color (get-in grid [row-num col-num])))
        ;(.fillText ctx  
                   ;(trans cell)
                   ;(* col-num cell-width) 
                   ;(* row-num cell-height))
        (.fillRect ctx  
                   (* col-num cell-width) 
                   (* row-num cell-height)
                   cell-width 
                   cell-height)
        (aset ctx "fillStyle" "white")))))

; Override with js implementation
(defn draw-grid [ctx [grid & [old-grid]] [cell-width cell-height]
                 & {:keys [redraw?]}]
  (js/Graphics.drawGrid ctx 
                        (clj->js grid) 
                        cell-width cell-height 
                        (clj->js old-grid)))

(defn clear [canvas]
  (.clearRect (.getContext canvas "2d")
              0 0 (.-width canvas) (.-height canvas)))

(defn slice-around [grid [row col :as position] size]
  (let [size (/ size 2)]
    (slice grid (+and- row size) (+and- col size))))

(defn slice [grid [from-row to-row] [from-col to-col]]
  (vectorify
    (for [row (range from-row to-row)]
      (for [col (range from-col to-col)]
        (get-in grid (map/wrap [row col] grid))))))

(defn grid-component [[grid [cell-width cell-height :as zoom] [left top] :as props] owner]
  (reify
    om/IDidMount
    (did-mount [this]
        (draw-grid (.getContext (om/get-node owner) "2d") [grid] [cell-width cell-height]))
    om/IRender
    (render [this]
      (html [:canvas {:width (* (count (first grid)) cell-width)
                      :height (* (count grid) cell-height)
                      :style {:position "absolute"
                              :top top
                              :left left}}]))
    om/IShouldUpdate
    (should-update [this prev-props prev-state]
      (not= props prev-props))
    om/IDidUpdate
    (did-update [this [prev-grid prev-zoom _] prev-state]
      (let [same-zoom? (= prev-zoom zoom)]
        (when-not (and (= prev-grid grid) same-zoom?)
          (draw-grid (.getContext (om/get-node owner) "2d")
                     [grid prev-grid] [cell-width cell-height]
                     :redraw? (not same-zoom?)))))))

(defn new-chunk [grid [row col] size]
  {:size size
   :center [row col]})

(defn chunk-id [chunk]
  (str "cell-" (join "," (chunk :center))))


(defn add-row [{:keys [chunks] :as state} grid direction chunk-size]
  (let [+or- (if (= direction :top) - +)
        ;Take the top or bottom left chunks as our starting point
        [base-row base-col] (:center (if (= direction :top)
                                       (first (first chunks))
                                       (first (last chunks))))
        chunks-per-row (count (first chunks))
        ; Create a new row with the proper shift form the starting point
        new-row (vec (for [col (take chunks-per-row (iterate (partial + chunk-size) base-col))]
                  (new-chunk grid [(+or- base-row chunk-size) col] chunk-size)))]
    ; Add to begging or end of grid
    (if (= direction :top)
      (-> state
          (update-in [:chunks] #(apply vector new-row %))
          (update-in [:chunks] (comp vec butlast)))
      (-> state
          (update-in [:chunks] #(conj % new-row))
          (update-in [:chunks] (comp vec rest))))))


(defn add-column [{:keys [chunks] :as state} grid direction chunk-size]
  (let [+or- (if (= direction :left) - +)
        ;Take the top or bottom left chunks as our starting point
        [base-row base-col] (:center (if (= direction :left)
                                       (first (first chunks))
                                       (last (first chunks))))
        chunks-per-col (count chunks)
        ; Create a new row with the proper shift form the starting point
        new-col (vec (for [row (take chunks-per-col (iterate (partial + chunk-size) base-row))]
                  (new-chunk grid [row (+or- base-col chunk-size)] chunk-size)))]
    ; Add to begging or end of grid
    (if (= direction :left)
      (-> state
          (update-in [:chunks] #(map cons new-col %))
          (update-in [:chunks] (partial mapv (comp vec butlast))))
      (-> state
          (update-in [:chunks] #(map conj % new-col))
          (update-in [:chunks] (partial mapv (comp vec rest)))))))
(defn inverse-direction [dir]
  (case dir 
    :top :bottom :bottom :top
    :left :right :right :left))

(defn shift-base [state direction chunk-size]
  (let [shift-direction (if (direction #{:left :right}) :left :top)
        +or- (if (= shift-direction direction) + -)]
        (update-in state [:base-shift shift-direction] +or- chunk-size)))


(defn shift-bounds [state direction chunk-size]
  (let [+or- (if (direction #{:left :top}) - +)
        inverse (case direction :top :bottom :bottom :top 
                  :left :right :right :left) ]
    (reduce #(update-in % [:bound %2] +or- chunk-size) 
            state [direction inverse])))

; TODO: Fix bounds and base fix.
(defn add-side [{:keys [chunks] :as state} grid direction chunk-size]
  (let [add-col-or-row (if (direction #{:top :bottom}) add-row add-column)]
    (-> state
        (add-col-or-row grid direction chunk-size)
        (shift-base direction chunk-size)
        (shift-bounds direction chunk-size))))

;TODO: updates bounds on chunk-count
(defn map-component [[grid [row col] 
                      & [{:keys [cell-width cell-height]
                          :or [cell-width 10 cell-height 10]}]]
                     owner props]
  (let [{:keys [chunk-size chunk-count
                viewport-height viewport-width] :as props}
        ; Set defaults not with destructuring becase :as and :or don't work well together.
        (merge {:chunk-size 7 :chunk-count 9
                :viewport-width 200 :viewport-height 200} props)
        chunk-width (* chunk-size cell-width)
        chunk-height (* chunk-size cell-height)]
    (reify
      ; Initial the grid around the center position given by
      ; surrounded the center with chunks.
      om/IInitState
      (init-state [this] 
        ; Determine chunk count on either side of center
        (let [chunks-per (/ (- chunk-count 1) 2)
              rows (range (- row (* chunks-per chunk-size)) 
                          (+ 1 row (* chunks-per chunk-size)) chunk-size)
              cols (range (- col (* chunks-per chunk-size))
                          (+ 1 col (* chunks-per chunk-size)) chunk-size)]
          {:chunks 
           (vectorify
             (for [row rows]
               (for [col cols]
                 (new-chunk grid [row col] chunk-size))))
           :bound {:left (- col chunk-size)
                   :right (+ col chunk-size)
                   :top (- row chunk-size)
                   :bottom (+ row chunk-size)}
           ; Shift the base by the chunks to the left and right.
           ; Will change as we add and remove chunks.
           ;
           ; Row and Col shifts are not included because they will be used later to center.
           ; This is to set up the correct frame of refernce.
           :base-shift {:top (- row) 
                        :left (- col)} }))
      ; Adjust scroll position after mount to center 
      om/IWillReceiveProps
      (will-receive-props [this [_ [next-row next-col]]]
        (let [state (om/get-state owner)
              {:keys [left right top bottom]} (state :bound)]
          ; Check to see if the viewport has moved out of the bounds covered
          ; by the currently rendered chunks.
          (om/update-state! owner
            #(cond-> %
               (< next-col left)
               (add-side grid :left chunk-size)
               (> next-col right)
               (add-side grid :right chunk-size)
               (< next-row top)
               (add-side grid :top chunk-size)
               (> next-row bottom)
               (add-side grid :bottom chunk-size)))))
      om/IDidMount
      (did-mount [this]
        (let [base-shift (:base-shift (om/get-state owner))
              canvas-height (* chunk-count chunk-size cell-height)
              canvas-width (* chunk-count chunk-size cell-width)]
          (doto (om/get-node owner) 
            (aset "scrollTop" (+ (* cell-height (+ row (:top base-shift)))
                                 (/ (- canvas-height viewport-height) 2)))
 
            (aset "scrollLeft" (+ (* cell-width (+ col (:left base-shift)))
                                  (/ (- canvas-width viewport-width) 2)
)))))
      om/IDidUpdate
      (did-update [this _ _]
        (let [base-shift (:base-shift (om/get-state owner))
              canvas-height (* chunk-count chunk-size cell-height)
              canvas-width (* chunk-count chunk-size cell-width)]
          (doto (om/get-node owner) 
            (aset "scrollTop" (+ (* cell-height (+ row (:top base-shift)))
                                 (/ (- canvas-height viewport-height) 2)))
 
            (aset "scrollLeft" (+ (* cell-width (+ col (:left base-shift)))
                                  (/ (- canvas-width viewport-width) 2)
)))))
      om/IRenderState
      (render-state [this {:keys [base-shift map-width map-height chunks]}]
        (html [:div {:id "map" 
                     :style {:width viewport-width 
                             :height viewport-width
                             :overflow "scroll"}}
               (flatten (for [[row-of-chunks row-num] (map list chunks (range))]
                 (for [[chunk col-num] (map list row-of-chunks (range))]
                    (om/build grid-component 
                              [(slice-around grid 
                                             (chunk :center) 
                                             (chunk :size))
                               [cell-width cell-height] 
                               [(* col-num chunk-width)
                                (* row-num chunk-height)] 
                               [chunk-width chunk-height]]
                              {:opts props :react-key (:center chunk)}))))])))))
