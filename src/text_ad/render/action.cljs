(ns text-ad.render.action
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [om.core :as om :include-macros true]
            [text-ad.actions :as actions]
            [sablono.core :refer-macros [html]]))

(declare 
  polymer-button
  action-button)

(defn view [state owner]
  (om/component
    (html [:div#actions 
           (for [action (actions/available? state)]
             (om/build action-button [action state] 
                       {:react-key (hash action)}))])))

(defn action-button [[[action-name {:keys [perform]}] state] owner]
  (om/component 
    (html
      (om/build polymer-button 
                {:label action-name
                 :on-click #(om/transact! state perform)}
                {:opts {:class "action-buton"}}))))

(defn polymer-button [{:keys [label on-click]} owner {:keys [class] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:bubble {:r 0 :x 0 :y 0}})
    om/IRenderState
    (render-state [_ {{:keys [x r y]} :bubble}]
      (html
        [:div {:class (str "polymer-button" " " class)
               :ref "bound"
               :on-mouse-down 
               #(let
                  [eventx (.-pageX %)
                   rect (.getBoundingClientRect (om/get-node owner "bound"))
                   x (- (.-pageX %) (.-left rect))
                   y (- (.-pageY %) (.-top rect))]
                  (om/set-state! owner :bubble {:x x :y y :r 200}))
               :on-mouse-up 
               #(do (on-click)
                    (do-in 200 
                           (om/set-state! owner :bubble {:x x :y y :r 0})
                           (om/set-state! owner :clicking? false)))}
         [:span label]
         [:div.ripple 
          {:ref "ripple"
           :style {:width (* r 2) :height (* r 2)
                   :margin-left (- r ) :margin-top (- r)
                   :left x :top y}}]]))))
