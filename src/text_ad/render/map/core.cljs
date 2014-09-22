(ns text-ad.render.map.core
  (:require [om.core :as om :include-macros true]
            [text-ad.map :refer [vectorify] :as map]
            [text-ad.util :refer [create-element! in-between?]]
            [clojure.string :refer [join]]
            [sablono.core :refer-macros [html]]))

(defprotocol Drawable (draw [this ]))

(defn trans [cell]
  (let [cell (if (keyword? cell) cell (keyword cell))]
    ({:grass "."
      :person "X"
      :mountain "A"  
      :river "~"
      :hills "^"
      :plains ","
      :forest "*"
      :water-temple "#"} cell)))

(defn trans-color ^:export [cell] 
  (let [ch (trans cell)]
    (if-not ch (do (print ch) (print cell)))
    (str "rgb(" 
         (-> ch (.charCodeAt 0) (* 75684) (mod 255)) ","
         (-> ch (.charCodeAt 0) (* 94231) (mod 255)) ","
         (-> ch (.charCodeAt 0) (* 31349) (mod 255)) ")")))

(defn pre-render-map [map-data & {:keys [chunk-size] :as options}]
  (let [cache (atom [])
        row-count (count map-data)
        col-count (count (first map-data))
        [row-segments col-segments]
        (for [v [row-count col-count]] (partition-all chunk-size (range v)))]
    (for [row-segment row-segments]
      (for [col-segment col-segments]
        (for [row row-segment
              col col-segment]
          [row col])))))


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


(defn map-view [state owner]
  (om/component
    (html [:div
           (om/build map-component 
                     [(state :map)
                      [(state :row) (state :col)] 
                      {:is-rendered? (state :is-rendered?)
                       :cell-height (state :zoom) 
                       :cell-width (state :zoom)}])
           (om/build +and-button state {:opts {:k :zoom}})
           (for [k [:row :col]]
             (om/build +and-button state {:opts {:k k}})) ])))

; Override with js implementation
(defn draw-grid! [ctx [grid & [old-grid]] [cell-width cell-height]
                 & {:keys [redraw?]}]
  (js/Graphics.drawGrid ctx 
                        (clj->js grid) 
                        cell-width cell-height 
                        (clj->js old-grid)
                        redraw?))

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
        (draw-grid! (.getContext (om/get-node owner) "2d") [grid] [cell-width cell-height]))
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
          (draw-grid! (.getContext (om/get-node owner) "2d")
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
                      & [{:keys [cell-width cell-height is-rendered?]
                          :or [cell-width 10 cell-height 10 is-rendered? false]}]]
                     owner props]
  (let [{:keys [chunk-size chunk-count
                viewport-height viewport-width] :as props}
        ; Set defaults not with destructuring becase :as and :or don't work well together.
        (merge {:chunk-size 9 :chunk-count 5
                :viewport-width 200 :viewport-height 200} props)]
    (reify
      ; Initial the grid around the center position given by
      ; surrounded the center with chunks.
      Drawable
      (draw [this]
        (let [{:keys [pre-render map-width map-height]} (om/get-state owner)
              row (mod row map-height)
              col (mod col map-width)
              viewable-rows (/ viewport-height cell-height)
              viewable-cols (/ viewport-width cell-width)
              x (- col (/ viewable-cols 2))
              y (- row (/ viewable-rows 2))
              ctx (.getContext (om/get-node owner "draw") "2d")
              imageData (.getImageData pre-render x y
                                       viewable-rows viewable-cols)
              tempCanvas (create-element! "canvas")]
          (.putImageData (.getContext tempCanvas "2d") imageData 0 0)
          (.save ctx)
          (aset ctx "webkitImageSmoothingEnabled" false)
          (.scale ctx (/ viewport-width viewable-cols)
                      (/ viewport-height viewable-rows))
          (.drawImage ctx tempCanvas 0 0)
          (.restore ctx)
          (aset ctx "fillStyle" "green")
          ))
      om/IInitState
      (init-state [this] 
        (let [map-width (count (first grid))
              map-height (count grid)]
          {:base-shift {:top (- row) 
                        :left (- col)}
           :map-width map-width
           :map-height map-height
           :pre-render 
           (let [canvas (create-element! "canvas" 
                          {:width 1000 
                           :height 1000})
                 ctx (.getContext canvas "2d")]
             (draw-grid! ctx [grid] [1 1])
             ctx)}))

      om/IDidMount
      (did-mount [this] (if is-rendered? (draw this)))
      om/IDidUpdate
      (did-update [this _ _] (if is-rendered? (draw this)))
      om/IRenderState
      (render-state [this {:keys [base-shift map-width map-height chunks]}]
        (html [:div {:id "map" 
                     :style {:width viewport-width 
                             :display "hidden"
                             :height viewport-width}}
               [:canvas {:ref "draw"
                         :width viewport-width
                         :height viewport-height}]])))))
