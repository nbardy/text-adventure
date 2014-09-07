(ns text-ad.render.inventory
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))

(defn view [inventory]
  (om/component
    (html
      [:table
       [:tbody
        (for [[k v] inventory] [:tr [:td k ":"] [:td v]])]])))
         

