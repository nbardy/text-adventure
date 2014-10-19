(ns text-ad.hook)

(defmacro defn-hookable [name arg-list & body]
  `(def ~name (text-ad.hook/new-hookable-function 
          (fn ~arg-list ~@body))))
