(ns text-ad.macros)

(defmacro do-in [ms & body]
  "Sets the body of code for execution in the future at least 
  arg: ms seconds from the current time."
  `(js/setTimeout (fn [] ~@body) ~ms))
  
(defmacro do-at [t & body]
  "Sets the body of code for execution in the future
  At some time after the give time"
  `(js/setTimeout (fn [] ~@body) (- ~t (js/Date.now))))

(defmacro defmap [name counter n bindings & grid]
  (let [num (if (= counter :cols) 
              n
              (/ (count grid) n))]
    `(def ~name 
       (let ~bindings
         (into [] (map (partial into [])
                       (partition ~num 
                                  (map #(%) [~@grid]))))))))

(defmacro defaction [action-name _ collection
                     available-body perform-body & [message-body]]
  "DSL for creating actions.
  Example Usage:
  (defaction \"Rub Eyes\" :in all
     ([state] true)
     ([state] (assoc state [:rubbed-eyes] true)))
  Accepts optional third message adding function.
  This function can return a sequence or a single string and they will be
  propely added to the message section of the state."
  (let [perform-function 
        (if message-body
          `(comp (fn [state#] 
                   (update-in state# [:messages] 
                    #(apply conj % (flatten (list 
                      ((fn ~@message-body) state#))))))
                 (fn ~@perform-body))
          `(fn ~@perform-body))]
    `(do
       (defonce ~collection (atom {}))
       (swap! ~collection assoc ~action-name 
              {:available? (fn ~@available-body)
               :perform ~perform-function}))))
