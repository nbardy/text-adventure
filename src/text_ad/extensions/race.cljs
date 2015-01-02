(ns text-ad.extensions.race
  (:require [text-ad.hook :refer [add-hook!]]
            [text-ad.extensions.core :refer [new-race!]]
            [text-ad.battle-actions :refer [dmg]]
            [text-ad.unit :refer [get-race]]))

(new-race! :dwarf
  :con 65
  :dex 20
  :wis 40
  :str 60
  :actions {:stomp (dmg 5)})

(new-race! :human
  :con 50
  :dex 50
  :wis 40
  :int 60
  :str 40
  :actions {:wave (dmg 0)})

(new-race! :elf
  :dex 60
  :con 10
  :wis 80
  :int 30
  :actions {:tickle (dmg 0)})
