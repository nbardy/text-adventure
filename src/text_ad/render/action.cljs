(ns text-ad.render.action
  (:require-macros [text-ad.macros :refer [do-in]])
  (:require [om.core :as om :include-macros true]
            [text-ad.render.components :refer [polymer-button]]
            [text-ad.actions :as actions]
            [sablono.core :refer-macros [html]]))

(declare action-button)

(defn view [state owner]
  (om/component
    (html [:div#actions 
           (for [action (actions/available? state)]
             (om/build action-button [action state] 
                       {:react-key (hash action)}))])))

(defn action-button [[[action-name {:keys [perform]}] state] owner]
  (om/component 
    (html
      [:div.action-button
       (om/build polymer-button 
                 {:label action-name
                  :on-click #(om/transact! state perform)})])))

