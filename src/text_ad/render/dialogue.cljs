(ns text-ad.render.dialogue
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]
            [text-ad.util :refer [css-trans-group]]
            [text-ad.render.components :refer [polymer-button]]))

(defn view [{:keys [dialogue] :as state} owner]
  (om/component
    (html
      [:div#dialogue-page
      (css-trans-group #js {:transitionName "blur-in"} 
                       (when dialogue (html [:div.blur-box ])))
      (css-trans-group #js {:transitionName "zoom-in"}
        (when dialogue
          (html 
            [:div#dialogue-box
            [:h4 (:name dialogue)]
            [:p (:desc dialogue)]
            (for [option (:options dialogue)]
              (om/build polymer-button 
                        {:label (:name option) 
                         :on-click (fn [_] (om/transact! state
                                            #(-> % ((:action option)) (dissoc :dialogue))))}))])))])))
