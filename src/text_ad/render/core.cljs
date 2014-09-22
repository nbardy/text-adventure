(ns text-ad.render.core
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]
            [text-ad.render.info :as info]
            [text-ad.render.action :as action]
            [text-ad.render.dialogue :as dialogue]
            [text-ad.render.messages :as messages]
            [text-ad.render.map.core :refer [map-view]]))

(defn app-view [app-state owner]
  (om/component 
    (html 
      [:div 
       [:div#info-page
        [:div#title (get app-state :title "?")]
        [:div#stats
         (when (get app-state :stats)
           (om/build info/stats-view (app-state :stats)))]
        [:div#inventory
         (when (get app-state :inventory) 
           (om/build info/inventory-view (app-state :inventory)))]]
       [:div#action-page (om/build action/view app-state)]
       [:div#map-page (om/build map-view (assoc app-state :is-rendered? true))]
       (om/build dialogue/view app-state)
       (when-not (empty? (get-in app-state [:messages]))
         [:div#message-page (om/build messages/view app-state)])])))
