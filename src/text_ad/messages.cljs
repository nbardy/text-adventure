(ns text-ad.messages)

(defn new [msg] [(js/Date.now) msg])
