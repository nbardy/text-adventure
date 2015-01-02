(ns text-ad.render.battle
  (:require-macros [text-ad.macros :refer [do-in]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :as async :refer [chan put! <! timeout]]
            [text-ad.battle :as battle :refer [alive? result next-turn]]
            [text-ad.util :refer [get-entity]]
            [clojure.string :as s]
            [text-ad.render.components :refer [polymer-button]]
            [sablono.core :refer-macros [html]]))

(defprotocol Drawable (draw! [this]))
(defprotocol Resetable (reset [this]))

(declare controls)

(def width 20)
(def height 20)
(def health-buffer 2)
(def health-color "lime")
(def target-width 5)
(def animation-length 250)
(def enemy-wait-time 400)

(defn bounce [t speed] 
  "A bouncing function f (t). |sin(t)
  Based on |sin(t)| with range: [0,1] and period: (speed)."
  (js/Math.abs (* 0.5 (js/Math.sin (* t (/ speed) js/Math.PI)))))

(defn wobble [t speed] 
  "A sine wave on t with range: [0,1], and a period of speed"
  (* 0.5 (- 1 (js/Math.cos (* t (/ speed) 2 js/Math.PI)))))

(defn cleanup-target [[side n] fight-state]
  (let [target ; Wrap the target
        (if (> n 0) ; Add close to avoid mod 0
          [side (mod n (count (side fight-state)))]
          [side 0])]
    ; Skip if dead
    (if (alive? (get-in fight-state target))
      target
      (cleanup-target [side (inc n)] fight-state))))


(defn next-target-fn [fight-state]
  (fn [[side n]]
    (cleanup-target [side (inc n)] fight-state)))

(def anim-speed 80)
(def triangle-height 6)
(def triangle-width 6)
(def buffer 0)

(defn point-to! [ctx x y t color ]
    (let [y (-> y (- triangle-height buffer (* 4 (bounce t 420))))
          shift (/ (- width triangle-width) 2)]
      (doto ctx
        (aset "fillStyle" color)
        (.beginPath)
        (.lineTo (+ x shift) y)
        (.lineTo (+ x shift triangle-width) y)
        (.lineTo (+ x shift (/ triangle-width 2)) (+ y triangle-height))
        (.fill)
        ;(.fillRect (+ x 8) (-> y (- 12) (+ (js/Math.sin (/ t anim-speed) 7))) 5 5)
        )))

(defn draw-square! [ctx x y color]
  (doto ctx
    (aset "fillStyle" color)
    (.fillRect x y width height)))

(defn draw-health! [ctx person x y ]
  (let [percent (max (/ (:health person) (:max-health person)) 0)]
    (doto ctx
      (aset "fillStyle" health-color)
      (.fillRect x y (* percent width) 6))))

(defn draw-unit! [ctx x y t color [person target animating active]
                  & {:keys [wobble-dir]}]
  ; Draw targeting riticule if active
  (if (= person target) (point-to! ctx x y t "black"))
  (condp #(= (:id %) %2) (:id person)
    animating 
    (draw-square! ctx (+ x (* 20 (or wobble-dir 1) 
                              (wobble t animation-length)))
                  y "green")
    active (draw-square! ctx x (+ y (- (* 5 (wobble t 600)))) color)
    (draw-square! ctx x y color))
  (draw-health! ctx person x (+ y health-buffer height)))

(defn view [{:keys [enemies allies turn] :as fight-state} owner {:keys [on-end]}]
  (let [turn (or turn 0)
        turns (battle/turns (concat allies enemies))
        enemies (filter alive? enemies)
        allies (filter alive? allies)
        perform-action! 
        (fn [active {:keys [effect]}]
          (let [{:keys [target animating t] :as st} 
                (om/get-state owner)
                res-chan (chan)]
            (when-not (or (result @fight-state) (boolean animating))
              (om/update-state! owner 
                #(merge % {:animating active
                           :animation-start-t t}))
              (do-in animation-length 
                     (om/set-state! owner :animating false)
                     (put! res-chan (om/transact! fight-state #(effect % target active)))
                     (when-not (result @fight-state)
                       (om/transact! fight-state next-turn))))
            res-chan))]
    (reify
      om/IInitState
      (init-state [_]
         {:target [:enemies 0]})
      Drawable
      (draw! [_] 
        (let [ctx (.getContext (om/get-node owner "canvas") "2d")
              {:keys [target t animating animation-start-t]} (om/get-state owner)
              active (get-entity (nth turns turn) (concat allies enemies))
              t (- t animation-start-t)
              target (get-in fight-state target)]
          (.clearRect ctx 0 0 200 200)
          (doseq [[person idx] (map list allies (range 1 100))]
            (draw-unit! ctx 0 (* idx 30) t "blue"
                         [person target animating active]))
          (doseq [[person idx] (map list enemies (range 1 100))]
            (draw-unit! ctx 160 (* idx 30) t "red"
                         [person target animating active]
                        :wobble-dir -1))))
      om/IWillMount
      (will-mount [_]
        (om/set-state! owner :animation 
          (js/setInterval #(om/set-state! owner :t (js/Date.now)) 10)))
      om/IDidMount
      (did-mount [this] 
        (draw! this))
      om/IWillUnmount
      (will-unmount [this]
        (js/clearInterval (om/get-state owner :animation)))
      om/IDidUpdate
      (did-update [this prev-fight-state prev-state] 
        ; Always draw
        (draw! this)
        (if (result fight-state) 
          (do-in 200 (on-end))
          ; Update values when it is a new turn.
          (when (not= (:turn prev-fight-state) turn)
            (if-let [active (get-entity (nth turns turn) enemies)]
              ; Control AI
              (let [ch-key (rand-nth (keys (get active :actions)))
                    chosen (get-in active [:actions ch-key])]
                (om/set-state! owner :target 
                               (cleanup-target [:allies 0] fight-state))
                (go 
                  ; Wait to give the impression the computer is deciding
                  (<! (timeout enemy-wait-time))
                  ; Perform the random action
                  (<! (perform-action! active @chosen))))
              ; If human controlled set target to enemies
              (om/set-state! owner :target (cleanup-target [:enemies 0] fight-state))))))
      om/IRenderState
      (render-state [_ {:keys [target animating t] :as state}]
        (html
          [:div {:style {:position "relative"}}
           [:canvas {:ref "canvas" :width 200 :height 200
                     :on-click #(if-not (result @fight-state)
                                  (om/update-state! owner :target 
                                    (next-target-fn @fight-state)))}]
           (let [active (get-entity (nth turns turn) (concat allies enemies))
                 disabled (get-entity (nth turns turn) enemies)]
             (when-not animating
               (for [[k action] (active :actions)]
                 (om/build polymer-button 
                           {:label (s/capitalize (name k))
                            :disabled disabled
                            :on-click #(perform-action! @active @action)}))))])))))
