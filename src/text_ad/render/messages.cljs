(ns text-ad.render.messages
  (:require-macros [text-ad.macros :refer [do-in do-at]])
  (:require [om.core :as om :include-macros true]
            [text-ad.actions :as actions]
            [text-ad.util :refer [css-trans-group]]
            [sablono.core :refer-macros [html]]))

(def min-time 500)
(defn long-enough? [t] 
  (> (- (js/Date.now) t) min-time))

(defn- next-id [coll]
  (inc (or (-> coll last :id) -1)))

(defn- next-time [coll]
  (let [t (or (-> coll last :t) 0)]
    ; If it has been long enough since last message it will appear now.
    ; Otherwise tell message to appear long enough after last messsage.
    (max (js/Date.now) (+ t min-time))))

(defn view [{:keys [messages] :as app-state} owner]
  (reify
    om/IWillMount
    (will-mount [_] true)
    om/IInitState
    (init-state [_]
      (let [with-meta (mapv (fn [id msg] {:id id :msg msg :t (js/Date.now)}) 
                            (range) messages)]
            {:all with-meta :available with-meta}))
    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (let [current-msgs (:all (om/get-state owner))
            new-msgs (drop (count current-msgs) (:messages next-props))
            all-with-meta 
            (reduce (fn [coll v] (conj coll {:msg v :id (next-id coll) 
                                             :t (next-time coll) }))
                   current-msgs new-msgs)
            new-msgs-with-meta (take-last (count new-msgs) all-with-meta)]
        (doseq [msg new-msgs-with-meta]
          (do-at (msg :t)
                 (om/update-state! owner :available #(conj % msg))))
        (om/set-state! owner :all all-with-meta)))
    om/IRenderState
    (render-state [_ state]
      (html 
        [:ul.message-list
         (apply css-trans-group #js {:transitionName "slide-down"}
                (for [msg-item (reverse (state :available))]
                  (html [:li {:key (:id msg-item)
                              :class "message"} 
                         [:div {:class "message-content"}
                          (:msg msg-item)]])))]))))


