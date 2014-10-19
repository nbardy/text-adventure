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



(defn partition-at [pred coll]
  "Partitions the coll into groups starting where pred returns true.
   WARNING: works for partitions dialogues for macros, but breaks in other cases."
  (let [separated (partition-by #(boolean (pred %)) coll)]
    (filter #(not (pred (last %))) (map concat separated (drop 1 separated)))))

(defmacro parse-dialogue-options [option-groups]
  "Accpets a list of unparsed dialogue options and parsess them into dialogue hashes
  with the form {:available (fn) :name (string) :action (fn)}"
  `(list ~@(for [group# option-groups]
             ; Each options defaults to available unless a function is provided.
             (let [fn-body (if (= (count group#) 3)
                             (second group#) `([_#] true))
                   option-name (first group#)
                   action-body (last group#)] 
               {:available? `(fn* ~fn-body)
                  :name option-name
                :action `(fn* ~action-body)}))))

(defmacro defdialogue [action-name _ collection & more]
  "Defines a dialogue action with the DSL defined in the accompanying documentation."
  (let [desc (if (= (first more) :description) (second more))
        more (if desc (drop 2 more) more)
        action-available (first more)
        dialogue-groups (partition-at string? more)
        dialogue-options `(parse-dialogue-options ~dialogue-groups)
        dialogue-action
        `([state#] 
           (assoc state# :dialogue 
                  {:name ~action-name
                   :desc ~desc
                   :options (filter #((:available? %) state#)
                          ~dialogue-options)}))]
    `(defaction ~action-name :in ~collection ~action-available ~dialogue-action)))
