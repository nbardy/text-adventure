(ns text-ad.macros)

(defmacro defmap [name counter n bindings & grid]
  (let [num (if (= counter :cols) 
              n
              (/ (count grid) n))]
    `(def ~name 
       (let ~bindings
         (into [] (map (partial into [])
                       (partition ~num 
                                  (map #(%) [~@grid]))))))))
