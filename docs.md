Attributions
------------
picnic.css buttons
google polymer buttons
a dark room game insipriation

DSLS
====
defaction
---------
Creates a generic action

Example Usage:
  (defaction \"Rub Eyes\" :in all
   ([state] true)
   ([state] (assoc state [:rubbed-eyes] true)))

defdialogue
-----------
defdialogue creates an action of the dialogue type
Arguments
Action Label(string)
Blank
Collection(symol/varname)
Optional description
Availibility function(fn)
Dialogue Group
  consists of a
  - name 
  - availability function(optional, defaults to true)
  - action function: what occurs when the dialogue button is pressed(fn)

A dialogue group is defined as such where
the

Example Usage:
(defdialogue "Check Mirror" :in all
  :description "Looking in the mirror the a face starers back at you. You recognize it as the face of a ..."
  ([state] (some #{:mirror} [:mirror]))
  "Human" ([state] true)
  ([state] (set-race state :human))
  "Dwarf" ([state] false)
  ([state] (set-race state :dwarf))
  "Elf" ([state] (set-race state :elf))
  "Orc" ([state] (set-race state :orc)))
