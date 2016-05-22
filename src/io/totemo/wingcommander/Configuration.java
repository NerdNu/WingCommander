package io.totemo.wingcommander;

import java.util.TreeMap;

import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    /**
     * Magnitude of the vertical acceleration applied on take-off in blocks per
     * tick per tick.
     */
    public double ACCELERATION_TAKEOFF_VERTICAL;

    /**
     * Magnitude of the acceleration in the look direction applied on take-off.
     */
    public double ACCELERATION_TAKEOFF_LOOK;

    /**
     * Magnitude of the acceleration applied when the player crouches during
     * flight.
     */
    public double ACCELERATION_LOOK;

    /**
     * Period in milliseconds for which the player's glide state will be set to
     * true after take-off.
     */
    public long TAKEOFF_GLIDE_MILLIS;

    /**
     * Maximum period in milliseconds between taps on the crouch key to trigger
     * a take-off.
     */
    public long TAKEOFF_TAP_MILLIS;

    /**
     * Maximum magnitude of the velocity vector, in blocks per tick, enforced
     * when power is applied.
     */
    public double MAX_VELOCITY;

    /**
     * Particle type of the exhaust trail, generated when power is applied, or
     * "none" to suppress particles.
     */
    public Effect EXHAUST_EFFECT;

    /**
     * Block type ID for particle effects that use one.
     */
    public int EXHAUST_ID;

    /**
     * Block data for particle effects that use a block type.
     */
    public int EXHAUST_DATA;

    /**
     * Maximum offset from the player position of spawned particles on all three
     * axes.
     */
    public float EXHAUST_OFFSET;

    /**
     * Speed of spawned particles.
     */
    public float EXHAUST_SPEED;

    /**
     * Number of spawned particles.
     */
    public int EXHAUST_COUNT;

    /**
     * Visibility radius of particles.
     */
    public int EXHAUST_RADIUS;

    /**
     * Engine sound when power is applied, or "none" to suppress sound.
     */
    public Sound EXHAUST_SOUND;

    /**
     * Volume of the engine sound.
     *
     * Sound range will be approximately this value times 15 blocks.
     */
    public float EXHAUST_VOLUME;

    /**
     * Engine sound when power is applied to broken wings, or "none" to suppress
     * sound.
     */
    public Sound BROKEN_SOUND;

    /**
     * Volume of the broken wing sound.
     *
     * Sound range will be approximately this value times 15 blocks.
     */
    public float BROKEN_VOLUME;

    /**
     * Allow the player to initiate 1 tick of glide by accelerating (crouching)
     * while falling with broken elytra.
     *
     * This allows a safe landing by power gliding, rather than just dropping
     * like a stone when elytra wear out.
     */
    public boolean BROKEN_GLIDE;

    /**
     * Reduce fall damage by this amount for each tick that power is applied
     * while falling on broken wings, but only if BROKEN_GLIDE is true.
     */
    public float BROKEN_GLIDE_FALL_REDUCTION;

    /**
     * If true, players can use the altimeter; otherwise it is not visible for
     * anybody.
     */
    public boolean ALTIMETER_ENABLED;

    /**
     * Altitude at which the altitude bar reads full.
     */
    public double ALTIMETER_CEILING;

    /**
     * Altimeter BossBar colours, indexed by the altitude threshold below which
     * they are active.
     */
    public TreeMap<Integer, BarColor> ALTIMETER_COLOURS = new TreeMap<Integer, BarColor>();

    /**
     * If true, players can use the speedometer; otherwise it is not visible for
     * anybody.
     */
    public boolean SPEEDOMETER_ENABLED;

    /**
     * Speed above which the speedometer reads full.
     */
    public double SPEEDOMETER_MAX;

    /**
     * Colour of the speedometer.
     */
    public BarColor SPEEDOMETER_COLOUR;

    /**
     * If true, vacuum asphyxiation damage is enabled.
     */
    public boolean VACUUM_ENABLED;

    /**
     * Altitude above which the player takes asphyxiation damage due to the
     * vacuum.
     */
    public double VACUUM_ALTITUDE;

    /**
     * Asphyxiation damage per tick due to the vacuum.
     */
    public double VACUUM_DAMAGE;

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        WingCommander.PLUGIN.reloadConfig();
        ACCELERATION_TAKEOFF_VERTICAL = WingCommander.PLUGIN.getConfig().getDouble("acceleration.takeoff.vertical");
        ACCELERATION_TAKEOFF_LOOK = WingCommander.PLUGIN.getConfig().getDouble("acceleration.takeoff.look");
        ACCELERATION_LOOK = WingCommander.PLUGIN.getConfig().getDouble("acceleration.look");

        TAKEOFF_GLIDE_MILLIS = WingCommander.PLUGIN.getConfig().getLong("takeoff.glide_millis");
        TAKEOFF_TAP_MILLIS = WingCommander.PLUGIN.getConfig().getLong("takeoff.tap_millis");

        MAX_VELOCITY = WingCommander.PLUGIN.getConfig().getDouble("max_velocity");

        String exhaustEffectName = WingCommander.PLUGIN.getConfig().getString("exhaust.effect");
        try {
            // Spigot Bug: Effect.getByName() does not recognise all Effect
            // names.
            EXHAUST_EFFECT = exhaustEffectName.equalsIgnoreCase("NONE") ? null : Effect.valueOf(exhaustEffectName);
        } catch (IllegalArgumentException ex) {
            WingCommander.PLUGIN.getLogger().warning("Invalid exhaust smoke effect name: \"" + exhaustEffectName + "\"");
            EXHAUST_EFFECT = null;
        }
        EXHAUST_ID = WingCommander.PLUGIN.getConfig().getInt("exhaust.id");
        EXHAUST_DATA = WingCommander.PLUGIN.getConfig().getInt("exhaust.data");
        EXHAUST_OFFSET = (float) WingCommander.PLUGIN.getConfig().getDouble("exhaust.offset");
        EXHAUST_SPEED = (float) WingCommander.PLUGIN.getConfig().getDouble("exhaust.speed");
        EXHAUST_COUNT = WingCommander.PLUGIN.getConfig().getInt("exhaust.count");
        EXHAUST_RADIUS = WingCommander.PLUGIN.getConfig().getInt("exhaust.radius");
        EXHAUST_SOUND = loadSound("exhaust.sound", "exhaust sound");
        EXHAUST_VOLUME = (float) WingCommander.PLUGIN.getConfig().getDouble("exhaust.volume");

        BROKEN_SOUND = loadSound("broken.sound", "broken elytra engine sound");
        BROKEN_VOLUME = (float) WingCommander.PLUGIN.getConfig().getDouble("broken.volume");
        BROKEN_GLIDE = WingCommander.PLUGIN.getConfig().getBoolean("broken.glide");
        BROKEN_GLIDE_FALL_REDUCTION = (float) WingCommander.PLUGIN.getConfig().getDouble("broken.glide_fall_reduction");

        ALTIMETER_ENABLED = WingCommander.PLUGIN.getConfig().getBoolean("altimeter.enabled");
        ALTIMETER_CEILING = WingCommander.PLUGIN.getConfig().getDouble("altimeter.ceiling");
        ALTIMETER_COLOURS.clear();
        ALTIMETER_COLOURS.put(999999, BarColor.PURPLE);
        ConfigurationSection altimeterColours = WingCommander.PLUGIN.getConfig().getConfigurationSection("altimeter.colours");
        for (String key : altimeterColours.getKeys(false)) {
            String value = altimeterColours.getString(key);
            try {
                int altitude = Integer.parseInt(key);
                ALTIMETER_COLOURS.put(altitude, BarColor.valueOf(value));
            } catch (NumberFormatException ex) {
                WingCommander.PLUGIN.getLogger().warning("Non-integer altitude value: " + key);
            } catch (IllegalArgumentException ex) {
                WingCommander.PLUGIN.getLogger().warning("Invalid altimeter colour: " + value);
            }
        }

        SPEEDOMETER_ENABLED = WingCommander.PLUGIN.getConfig().getBoolean("speedometer.enabled");
        SPEEDOMETER_MAX = WingCommander.PLUGIN.getConfig().getDouble("speedometer.max");
        String speedometerColour = WingCommander.PLUGIN.getConfig().getString("speedometer.colour");
        try {
            SPEEDOMETER_COLOUR = BarColor.valueOf(speedometerColour);
        } catch (IllegalArgumentException ex) {
            WingCommander.PLUGIN.getLogger().warning("Invalid speedometer colour: " + speedometerColour);
            SPEEDOMETER_COLOUR = BarColor.BLUE;
        }

        VACUUM_ENABLED = WingCommander.PLUGIN.getConfig().getBoolean("vacuum.enabled");
        VACUUM_ALTITUDE = WingCommander.PLUGIN.getConfig().getDouble("vacuum.altitude");
        VACUUM_DAMAGE = WingCommander.PLUGIN.getConfig().getDouble("vacuum.damage");
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Load a sound from the String configuration value at the given path,
     * interpreting NONE as null.
     *
     * Log a warning if the sound name in the configuration is invalid.
     *
     * @param path the path to the configuration string.
     * @param description how to describe the sound in any logged warning.
     * @return the Sound, or null for NONE or invalid names.
     */
    protected Sound loadSound(String path, String description) {
        String soundName = WingCommander.PLUGIN.getConfig().getString(path);
        try {
            return soundName.equalsIgnoreCase("NONE") ? null : Sound.valueOf(soundName);
        } catch (IllegalArgumentException ex) {
            WingCommander.PLUGIN.getLogger().warning("Invalid " + description + " name: \"" + soundName + "\"");
            return null;
        }
    }
} // class Configuration