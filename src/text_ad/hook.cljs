(ns text-ad.hook)

(defn- compose-hooks [f1 f2]
  (fn [& args]
    ;; TODO: tracing
    (apply f2 f1 args)))

(defn- join-hooks [original hooks]
  (reduce compose-hooks original hooks))

(defn- run-hooks [hooks original args]
  (apply (join-hooks original hooks) args))

(defrecord HookableFn [f hooks]
  cljs.core/IFn
  ; Still trying to figure out if there is a way around this
  (-invoke [this & args] 
    (run-hooks (deref (:hooks this))
               (:f this)
               args)))

(defn new-hookable-function [f]
  (HookableFn. f (atom [])))

(defn add-hook! [hookable hook]
  (swap! (:hooks hookable) conj hook))
