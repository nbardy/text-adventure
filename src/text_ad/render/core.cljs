(ns text-ad.render.core
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]
            [text-ad.util :refer [css-trans-group]]
            [text-ad.render.info :as info]
            [text-ad.render.mode :as mode]
            [text-ad.render.action :as action]
            [text-ad.render.dialogue :as dialogue]
            [text-ad.render.messages :as messages]
            [text-ad.render.map.core :refer [map-view]]))

(defn app-view [app-state owner]
  (om/component 
    (html 
      [:div 
       [:div#info-page
        (apply css-trans-group #js {:transitionName "fade-in"}
        (filter (complement nil?)
         [(when (get app-state :title)
            (html [:div#title (get app-state :title)]))
          (when (get app-state :stats)
            (html [:div#stats
                   (om/build info/stats-view (app-state :stats))]))
          (when (get app-state :inventory) 
            (html [:div#inventory
                   (om/build info/inventory-view (app-state :inventory))]))]))]
       [:div#action-page (om/build action/view app-state)]
       [:div#mode-page (om/build mode/view app-state)]
       (om/build dialogue/view app-state)
       (css-trans-group #js {:transitionName "fade-in"}
         (when-not (empty? (get-in app-state [:messages]))
           (html [:div#message-page (om/build messages/view app-state)])))])))
