(ns text-ad.render.components
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [om.core :as om :include-macros true]
            [text-ad.actions :as actions]
            [clojure.string :refer [join]]
            [sablono.core :refer-macros [html]]))

(defprotocol Resetable (reset [this]))
(defn polymer-button [{:keys [label on-click]} owner {:keys [class] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:bubble {:r 0 :x 0 :y 0}
       :clicking? false})
    Resetable
    (reset [this]
      (om/set-state! owner [:bubble :r] 0)
      (om/set-state! owner :clicking? false))
    om/IRenderState
    (render-state [this {{:keys [x r y]} :bubble :as state :keys [clicking?]}]
      (html
        [:div.polymer-button-wrapper
         [:div {:class (join " " [ "polymer-button" (if clicking? "" "") class])
                :ref "bound"
                :on-mouse-down 
                #(let [eventx (.-pageX %)
                       rect (.getBoundingClientRect (om/get-node owner "bound"))
                       x (- (.-pageX %) (.-left rect))
                       y (- (.-pageY %) (.-top rect))]
                   (om/set-state! owner {:clicking? true :bubble {:x x :y y :r 200}}))
                :on-mouse-up 
                #(do (om/set-state! owner :clicking? false)
                     (on-click)
                     (do-in 200 (reset this)))
                :on-mouse-leave
                #(when clicking?
                   (do-in 200 (reset this)))}
          [:span label]
          [:div.ripple 
           {:ref "ripple"
            :style {:width (* r 2) :height (* r 2)
                    :margin-left (- r ) :margin-top (- r)
                    :left x :top y}}]]]))))
