WingCommander
=============
A Minecraft 1.9 Bukkit plugin that makes elytra capable of powered flight.


Features
--------


Controls
--------
Double-tapping the crouch key gives the player an initial boost to take off.
Take off power is the sum of an acceleration in the player's look direction and
a vertical acceleration.

Holding the crouch key in flight increases the player's speed.

While the sprint or jump keys might be more intuitive keys to use for flight,
neither triggers a suitable event at the server.  (The sprint key only triggers
an event when moving forward on the ground.)


Commands
--------

 * `/wingcommander reload` - Reload the plugin configuration.


Permissions
-----------

 * `wingcommander.fly` - Permission to use powered flight. Without this
   permission, elytra can still be used in their default mode, to glide.
 * `wingcommander.admin` - Permission to administer the plugin (run `/wingcommander reload`).
