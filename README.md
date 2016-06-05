WingCommander
=============
A Minecraft 1.9 Bukkit plugin that makes elytra capable of powered flight.

![WingCommander 1.6.0](https://raw.github.com/totemo/WingCommander/master/images/screenshot.png)


Features
--------

 * Requires no commands for powered flight.  The crouch/sneak key is used
   as the accelerator when the player is off the ground and wearing elytra.
 * Optionally shows the player a colour-coded altimeter when in powered flight.
 * Optionally shows the player a speedometer (in blocks/sec) when in powered flight.
 * Optionally shows the player a wingometer (percentage wing durability remaining)
   when in powered flight.
 * Optionally damages the player above a configurable altitude (due to the lack
   of breathable air).
 * The visibility of gauges can be controlled by the `/gauge` command and is
   persistent across login sessions.
 * Virtually all aspects of flight dynamics and displays are configurable.


Controls
--------
Double-tapping the crouch key gives the player an initial boost to take off.
Take off power is the sum of an acceleration in the player's look direction and
a vertical acceleration.  It is also possible to take off by jumping while
crouched.

Holding the crouch key in flight increases the player's speed, up to the
configured maximum.

While the sprint or jump keys might be more intuitive keys to use for flight,
neither triggers a suitable event at the server.  (The sprint key only triggers
an event when moving forward on the ground.)


Commands
--------

 * `/wingcommander reload` - Reload the plugin configuration.
 * `/gauge [altitude|speed|wings] [off|on]` - Toggle or set visibility of a specific gauge or all gauges.


Configuration
-------------

| Setting | Description |
| :--- | :--- |
| `acceleration.look` | Acceleration in the look direction when thrust is applied. (blocks/tick/tick) |
| `acceleration.takeoff.vertical` | Vertical acceleration on takeoff (double-crouch). |
| `acceleration.takeoff.look` | Acceleration on takeoff (double-crouch) in the look direction. |
| `takeoff.glide_millis` | Milliseconds after takeoff for which glide mode is forced. |
| `takeoff.tap_millis` | Maximum period in milliseconds between taps on the crouch key to trigger a take-off. |
| `max_velocity` | Maximum velocity in blocks per tick when thrust is applied. |
| `exhaust.effect` | Particle type of the thruster exhaust, or NONE. |
| `exhaust.id` | Block type ID for particle effects that use one. |
| `exhaust.data` | Block data for particle effects that use a block type. |
| `exhaust.offset` | Maximum offset from the player position of spawned particles on all three axes. |
| `exhaust.speed` | Speed of spawned particles. |
| `exhaust.count` | Number of spawned particles. |
| `exhaust.radius` | Visibility radius of particles. |
| `exhaust.sound` | Sound played when thrust is applied, or NONE. |
| `exhaust.volume` | Volume of the engine sound (range is about 15 times this many blocks). |
| `broken.sound` | Sound played when thrust is applied to broken wings, or NONE. |
| `broken.volume` | Volume of the broken wing sound (range is about 15 times this many blocks). |
| `broken.glide` | If true, gliding to a safe landing on broken wings is possible when thrust is applied. |
| `broken.glide_fall_reduction` | Reduction in fall damage per tick that power is applied while falling on broken wings. |
| `altimeter.enabled` |  If true, players can use the altimeter; otherwise it is not visible for anybody. |
| `altimeter.ceiling` | Altitude at which the altitude bar reads full. |
| `altimeter.colours` | Map from integer (quoted as string) altitude to bar colour (BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW). For each number, the specified colour is shown below that altitude value. |
| `speedometer.enabled` | If true, players can use the speedometer; otherwise it is not visible for anybody. |
| `speedometer.max` | Speed above which the speedometer reads full. |
| `speedometer.colour` | Colour of the speedometer. |
| `wingometer.enabled` |  If true, players can use the wingometer; otherwise it is not visible for anybody. |
| `wingometer.colours` | Map from integer (quoted as string) percentage wing durability remaining to bar colour (BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW). For each number, the specified colour is shown when wing durability falls below that percentage. |
| `vacuum.enabled`	| If true, vacuum asphyxiation damage is enabled. |
| `vacuum.altitude` | Altitude above which the player takes asphyxiation damage due to the vacuum. |
| `vacuum.damage` | Asphyxiation damage per tick due to the vacuum. Note: in reality, damage cool downs prevent this from happening on every tick. |


Permissions
-----------

 * `wingcommander.fly` - Permission to use powered flight and `/gauge`.
   Without this permission, elytra can still be used in their default mode, to glide.
 * `wingcommander.admin` - Permission to administer the plugin (run `/wingcommander reload`).
