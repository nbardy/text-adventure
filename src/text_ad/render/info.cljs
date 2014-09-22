(ns text-ad.render.info
  (:require [om.core :as om :include-macros true]
            [clojure.string :as s]
            [sablono.core :refer-macros [html]]))

(defn inventory-view [inventory]
  (om/component
    (html
      [:table
       [:tbody
        (for [[k v] inventory] [:tr [:td k ":"] [:td v]])]])))
         
(defn stats-view [stats owner]
  (om/component
    (html 
      [:div 
       (when (stats :race) 
         [:div "Race: " (s/capitalize (name (stats :race)))])])))
