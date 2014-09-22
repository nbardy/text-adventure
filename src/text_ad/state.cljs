(ns text-ad.state)

(defn nearby-items [{:keys [row col grid] :as state}]
  [:mirror])

(defn set-race [state race] 
  (assoc-in state [:stats :race] race))
