(ns text-ad.render.battle
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [om.core :as om :include-macros true]
            [clojure.string :as s]
            [text-ad.render.components :refer [polymer-button]]
            [sablono.core :refer-macros [html]]))

(defprotocol Drawable (draw [this]))

(declare controls)

(def width 20)
(def height 20)

(defn highlight-if-target [ctx x y person target]
  (if (= person target)
    (doto ctx
      (aset "fillStyle" "red")
      (.fillRect (dec x) (dec y) (+ width 2) (+ height 2)))))

(defn draw-row [ctx people x & {:keys [target]}]
  (doseq [[person idx] (map list people (range 1 100))]
    (doto ctx
      (highlight-if-target x (* idx 30) person target)
      (aset "fillStyle" "blue")
      (.fillRect x (* idx 30) width height)
      (.fillText (person :health) x (+ (* idx 30) 30)))))

(defn next-target [fight-state]
  (fn [t] (update-in t [1] #(-> % (inc) (mod (count (:enemies fight-state)))))))

(defn view [{:keys [actions enemies allies] :as fight-state} owner {:keys [on-end]}]
  (reify
    om/IInitState
    (init-state [_]
      {:target [:enemies 0]})
    Drawable
    (draw [_] 
      (let [ctx (.getContext (om/get-node owner "canvas") "2d")
            target (get-in fight-state (om/get-state owner :target))]
        (.clearRect ctx 0 0 200 200)
        (draw-row ctx enemies 160 :target target)
        (draw-row ctx allies 20 :target target)))
    om/IDidMount
    (did-mount [this] (draw this))
    om/IDidUpdate
    (did-update [this _ _] 
      ; Wrap around target if overflowed
      (if (<= (reduce #(+ % (%2 :health)) 0 enemies) 0)
        (do
          (om/set-state! owner :victory true)
          (do-in 200 (on-end)))
        (om/update-state! owner :target 
                          (fn [[t n]] [t (mod n (count (t fight-state)))])))
      (draw this))
    om/IRenderState
    (render-state [_ {:keys [victory target]}]
      (html
        [:div {:style {:position "relative"}}
         [:canvas {:ref "canvas" :width 200 :height 200
                   :on-click #(om/update-state! owner [:target 1] inc)}]
         (for [[k action] actions]
           (om/build polymer-button 
                     {:label (s/capitalize (name k))
                      :on-click (fn [e] (om/transact! fight-state #(action % target)))}))]))))
