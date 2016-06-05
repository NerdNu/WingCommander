package io.totemo.wingcommander;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

// ----------------------------------------------------------------------------
/**
 * Transient, per-player state, created on join and removed when the player
 * leaves.
 */
public class PlayerState {
    /**
     * Constructor.
     *
     * @param player the player.
     * @param config the configuration from which player preferences are loaded.
     */
    public PlayerState(Player player, YamlConfiguration config) {
        _player = player;
        _altitudeBossBar = Bukkit.getServer().createBossBar("Altitude", BarColor.BLUE, BarStyle.SEGMENTED_20);
        _altitudeBossBar.addPlayer(_player);
        _altitudeBossBar.setProgress(0);
        _altitudeBossBar.setVisible(false);
        _speedBossBar = Bukkit.getServer().createBossBar("Speed", WingCommander.CONFIG.SPEEDOMETER_COLOUR, BarStyle.SEGMENTED_20);
        _speedBossBar.addPlayer(_player);
        _speedBossBar.setProgress(0);
        _speedBossBar.setVisible(false);
        _wingsBossBar = Bukkit.getServer().createBossBar("Wings", BarColor.GREEN, BarStyle.SEGMENTED_20);
        _wingsBossBar.addPlayer(_player);
        _wingsBossBar.setProgress(1.0);
        _wingsBossBar.setVisible(false);
        load(config);
    }

    // ------------------------------------------------------------------------
    /**
     * Handle a physics tick.
     */
    public void onTick() {
        // During take-off, force glide.
        long now = System.currentTimeMillis();
        if (now - _takeOffTime < WingCommander.CONFIG.TAKEOFF_GLIDE_MILLIS) {
            _player.setGliding(true);
        }

        // If a player loses glide in flight, let them glide again in the air
        // by pressing crouch.
        if (_player.isSneaking() && WingCommander.isFlightCapable(_player) && !_player.isOnGround() && !_player.isFlying()) {
            accelerate(WingCommander.CONFIG.ACCELERATION_LOOK);
        }

        updateBossBars();
        checkVacuumSuffocation();
    } // onTick

    // ------------------------------------------------------------------------
    /**
     * Handle the crouch key by detecting double-tap as a request to take off.
     *
     * The caller must ensure that the player is flight capable before calling
     * this method.
     */
    public void onCrouch() {
        if (_player.isOnGround()) {
            long now = System.currentTimeMillis();
            if (now - _lastCrouchTime < WingCommander.CONFIG.TAKEOFF_TAP_MILLIS) {
                _lastCrouchTime = 0;
                _takeOffTime = now;
                if (!areElytraBroken()) {
                    _player.setVelocity(new Vector(0, WingCommander.CONFIG.ACCELERATION_TAKEOFF_VERTICAL, 0));
                }
                accelerate(WingCommander.CONFIG.ACCELERATION_TAKEOFF_LOOK);
            } else {
                _lastCrouchTime = now;
            }
        }
    } // onCrouch

    // ------------------------------------------------------------------------
    /**
     * Set or toggle visibility of the altimeter.
     *
     * @param visibility the new visibility state, or null to toggle.
     */
    public void showAltimeter(Boolean visibility) {
        _showAltimeter = (visibility == null) ? !_showAltimeter : visibility;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the altimeter is shown.
     *
     * @return true if the altimeter is shown.
     */
    public boolean isAltimeterShown() {
        return _showAltimeter;
    }

    // ------------------------------------------------------------------------
    /**
     * Set or toggle visibility of the speedometer.
     *
     * @param visibility the new visibility state, or null to toggle.
     */
    public void showSpeedometer(Boolean visibility) {
        _showSpeedometer = (visibility == null) ? !_showSpeedometer : visibility;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the speedometer is shown.
     *
     * @return true if the speedometer is shown.
     */
    public boolean isSpeedometerShown() {
        return _showSpeedometer;
    }

    // ------------------------------------------------------------------------
    /**
     * Set or toggle visibility of the wingometer.
     *
     * @param visibility the new visibility state, or null to toggle.
     */
    public void showWingometer(Boolean visibility) {
        _showWingometer = (visibility == null) ? !_showWingometer : visibility;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the wingometer is shown.
     *
     * @return true if the wingometer is shown.
     */
    public boolean isWingometerShown() {
        return _showWingometer;
    }

    // ------------------------------------------------------------------------
    /**
     * Save this player's preferences to the specified configuration.
     *
     * @param config the configuration to update.
     */
    public void save(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        section.set("name", _player.getName());
        section.set("altimeter", _showAltimeter);
        section.set("speedometer", _showSpeedometer);
        section.set("wingometer", _showWingometer);
    }

    // ------------------------------------------------------------------------
    /**
     * Load the Player's preferences from the specified configuration
     *
     * @param config the configuration from which player preferences are loaded.
     */
    protected void load(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        if (section == null) {
            section = config.createSection(_player.getUniqueId().toString());
        }
        _showAltimeter = section.getBoolean("altimeter", true);
        _showSpeedometer = section.getBoolean("speedometer", true);
        _showWingometer = section.getBoolean("wingometer", true);
    }

    // ------------------------------------------------------------------------
    /**
     * Update BossBars according to the player's state and the configuration.
     */
    protected void updateBossBars() {
        boolean glidingAndPermitted = _player.isGliding() && _player.hasPermission("wingcommander.gauge");

        // If you show the altimeter when the player has equipped elytra and
        // not on the ground, every little jump will flash the altimeter.
        // Test for gliding instead.
        if (glidingAndPermitted && WingCommander.CONFIG.ALTIMETER_ENABLED && _showAltimeter) {
            double altitude = _player.getLocation().getY();
            _altitudeBossBar.setColor(WingCommander.CONFIG.getBarColor(WingCommander.CONFIG.ALTIMETER_COLOURS, (int) altitude));
            _altitudeBossBar.setTitle(String.format("Altitude: %d", (int) altitude));
            _altitudeBossBar.setProgress(Math.min(1.0, Math.max(0.0, altitude / WingCommander.CONFIG.ALTIMETER_CEILING)));
            _altitudeBossBar.setVisible(true);
        } else {
            _altitudeBossBar.setVisible(false);
        }

        if (glidingAndPermitted && WingCommander.CONFIG.SPEEDOMETER_ENABLED && _showSpeedometer) {
            double speed = _player.getVelocity().length();
            _speedBossBar.setTitle(String.format("Speed: %3.1f", 20 * speed));
            _speedBossBar.setProgress(Math.min(1.0, Math.max(0.0, speed / WingCommander.CONFIG.SPEEDOMETER_MAX)));
            _speedBossBar.setVisible(true);
        } else {
            _speedBossBar.setVisible(false);
        }

        if (glidingAndPermitted && WingCommander.CONFIG.WINGOMETER_ENABLED && _showWingometer) {
            ItemStack chest = _player.getEquipment().getChestplate();
            int remainingDurability = Material.ELYTRA.getMaxDurability() - chest.getDurability();
            double fraction = remainingDurability / (double) Material.ELYTRA.getMaxDurability();
            int percentage = (int) (100 * fraction);
            _wingsBossBar.setColor(WingCommander.CONFIG.getBarColor(WingCommander.CONFIG.WINGOMETER_COLOURS, percentage));
            _wingsBossBar.setTitle(String.format("Wings: %d%%", percentage));
            _wingsBossBar.setProgress(Math.min(1.0, Math.max(0.0, fraction)));
            _wingsBossBar.setVisible(true);
        } else {
            _wingsBossBar.setVisible(false);
        }
    } // updateBossBars

    // ------------------------------------------------------------------------
    /**
     * Inflict one tick's worth of asphyxiation damage from the vacuum if the
     * player flies too high.
     */
    protected void checkVacuumSuffocation() {
        if (WingCommander.CONFIG.VACUUM_ENABLED &&
            _player.getLocation().getY() >= WingCommander.CONFIG.VACUUM_ALTITUDE) {
            _player.damage(WingCommander.CONFIG.VACUUM_DAMAGE);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the player is not wearing eltra, or if they are too
     * damaged to glide.
     *
     * @return true if the player is not wearing eltra, or if they are too
     *         damaged to glide.
     */
    protected boolean areElytraBroken() {
        ItemStack chest = _player.getEquipment().getChestplate();
        return chest == null ||
               chest.getType() != Material.ELYTRA ||
               Material.ELYTRA.getMaxDurability() - chest.getDurability() < 1;
    }

    // ------------------------------------------------------------------------
    /**
     * Boost the player's velocity in the player's look direction, but limit the
     * magnitude of the velocity to the configured maximum.
     *
     * When power is applied and the elytra are not broken, glide is enabled. If
     * the elytra are broken, only glide if configured to do so.
     *
     * Also show particle effects and play the engine sound. If the elytra are
     * too damaged to function, no acceleration is applied and a different
     * engine sound is played.
     *
     * The caller must ensure that the player is wearing powered elytra before
     * calling this method.
     *
     * @param acceleration acceleration to apply in the player's look direction.
     */
    protected void accelerate(double acceleration) {
        Location loc = _player.getLocation();

        // Base the pitch on the current (not new) speed. Good enough.
        // Pitch is limited to the [0.5, 2.0] range in the client, apparently.
        // Dot product measures whether player is moving in the direction they
        // look.
        Vector velocity = _player.getVelocity();
        Vector look = _player.getLocation().getDirection();
        float speedFraction = (float) (Math.max(0.0, velocity.dot(look)) / WingCommander.CONFIG.MAX_VELOCITY);
        float pitch = 0.5f + 1.5f * speedFraction;

        if (areElytraBroken()) {
            loc.getWorld().playSound(loc, WingCommander.CONFIG.BROKEN_SOUND,
                                     WingCommander.CONFIG.BROKEN_VOLUME, pitch);
            if (WingCommander.CONFIG.BROKEN_GLIDE) {
                _player.setGliding(true);
                _player.setFallDistance(Math.max(0, _player.getFallDistance() - WingCommander.CONFIG.BROKEN_GLIDE_FALL_REDUCTION));
            }
        } else {
            _player.setGliding(true);

            Vector boost = look;
            boost.multiply(acceleration);
            velocity.add(boost);
            if (velocity.length() > WingCommander.CONFIG.MAX_VELOCITY) {
                velocity.multiply(WingCommander.CONFIG.MAX_VELOCITY / velocity.length());
            }
            _player.setVelocity(velocity);

            if (WingCommander.CONFIG.EXHAUST_EFFECT != null) {
                loc.getWorld().spigot().playEffect(loc,
                                                   WingCommander.CONFIG.EXHAUST_EFFECT,
                                                   WingCommander.CONFIG.EXHAUST_ID,
                                                   WingCommander.CONFIG.EXHAUST_DATA,
                                                   WingCommander.CONFIG.EXHAUST_OFFSET,
                                                   WingCommander.CONFIG.EXHAUST_OFFSET,
                                                   WingCommander.CONFIG.EXHAUST_OFFSET,
                                                   WingCommander.CONFIG.EXHAUST_SPEED,
                                                   WingCommander.CONFIG.EXHAUST_COUNT,
                                                   WingCommander.CONFIG.EXHAUST_RADIUS);
            }

            if (WingCommander.CONFIG.EXHAUST_SOUND != null) {
                loc.getWorld().playSound(loc, WingCommander.CONFIG.EXHAUST_SOUND,
                                         WingCommander.CONFIG.EXHAUST_VOLUME, pitch);
            }
        }
    } // accelerate

    // ------------------------------------------------------------------------
    /**
     * The Player.
     */
    protected Player _player;

    /**
     * Time stamp of the player's last crouch start.
     */
    protected long _lastCrouchTime;

    /**
     * Time stamp of the player's last take off.
     */
    protected long _takeOffTime;

    /**
     * BossBar used to display the player's altitude.
     */
    protected BossBar _altitudeBossBar;

    /**
     * BossBar used to display the player's speed.
     */
    protected BossBar _speedBossBar;

    /**
     * BossBar used to display the player's wing durability percentage.
     */
    protected BossBar _wingsBossBar;

    /**
     * If true, the altimeter is visible (notwithstanding other requirements for
     * it to be shown).
     */
    protected boolean _showAltimeter;

    /**
     * If true, the speedometer is visible (notwithstanding other requirements
     * for it to be shown).
     */
    protected boolean _showSpeedometer;

    /**
     * If true, the wingometer is visible (notwithstanding other requirements
     * for it to be shown).
     */
    protected boolean _showWingometer;

} // class PlayerState