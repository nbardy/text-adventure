(ns text-ad.render.mode
  (:require [om.core :as om :include-macros true]
            [text-ad.util :refer [mode-is?]]
            [text-ad.render.map.core :refer [map-view]]
            [text-ad.render.battle :as battle]
            [sablono.core :refer-macros [html]]))

(defn set-mode! [state mode] (om/update! state :mode :walking))

(defn view [app-state]
  (om/component 
    (html
      [:div.slider
       [:.item {:class (if (mode-is? app-state :walking) "active")}
        (om/build map-view (assoc app-state :is-rendered? true))]
       [:.item {:class (if (mode-is? app-state :fighting) "active")}
        (case (app-state :fight)
          :victory [:.h1 "Victory"]
          :loss [:.h1 "Loss"]
          (when (app-state :fight)
            (om/build battle/view (app-state :fight)
                    {:opts {:on-end #(set-mode! app-state :walking)}})))]])))
