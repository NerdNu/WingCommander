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
        _pitchBossBar = Bukkit.getServer().createBossBar("Pitch", BarColor.GREEN, BarStyle.SEGMENTED_20);
        _pitchBossBar.addPlayer(_player);
        _pitchBossBar.setProgress(0.5);
        _pitchBossBar.setVisible(false);
        load(config);
    }

    // ------------------------------------------------------------------------
    /**
     * Handle a physics tick.
     */
    public void onTick() {
        // During take-off, force glide.
        if (isTakingOff()) {
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
                setTakingOff();
                if (WingCommander.isWearingElytra(_player, true)) {
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
     * Signify that the player has initiated a take-off.
     * 
     * The start time of the take-off is recorded, and glide is forced for the
     * configured time period thereafter in {@link PlayerState#onTick()}.
     */
    public void setTakingOff() {
        _takeOffTime = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the player is still in the time-limited take-off state.
     * 
     * @return true if the player is still in the time-limited take-off state.
     */
    public boolean isTakingOff() {
        long now = System.currentTimeMillis();
        return (now - _takeOffTime < WingCommander.CONFIG.TAKEOFF_GLIDE_MILLIS);
    }

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
     * Set or toggle visibility of the pitch meter.
     *
     * @param visibility the new visibility state, or null to toggle.
     */
    public void showPitchmeter(Boolean visibility) {
        _showPitchmeter = (visibility == null) ? !_showPitchmeter : visibility;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the pitch meter is shown.
     *
     * @return true if the pitch meter is shown.
     */
    public boolean isPitchmeterShown() {
        return _showPitchmeter;
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
        section.set("pitchmeter", _showPitchmeter);
    }

    // ------------------------------------------------------------------------
    /**
     * Load the Player's preferences from the specified configuration
     *
     * @param config the configuration from which player preferences are loaded.
     */
    public void load(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        if (section == null) {
            section = config.createSection(_player.getUniqueId().toString());
        }
        _showAltimeter = section.getBoolean("altimeter", true);
        _showSpeedometer = section.getBoolean("speedometer", true);
        _showWingometer = section.getBoolean("wingometer", true);
        _showPitchmeter = section.getBoolean("pitchmeter", true);
    }

    // ------------------------------------------------------------------------
    /**
     * Update BossBars according to the player's state and the configuration.
     */
    protected void updateBossBars() {
        boolean gaugesMayActivate = _player.isGliding() && _player.hasPermission("wingcommander.gauge");

        // Debounce gauge activation.
        if (gaugesMayActivate) {
            if (!_gaugesPossible) {
                long now = System.currentTimeMillis();
                if (_glideStartTime == 0) {
                    _glideStartTime = now;
                } else if (now - _glideStartTime > WingCommander.CONFIG.GAUGE_DEBOUNCE_MILLIS) {
                    _gaugesPossible = true;
                }
            }
        } else {
            _gaugesPossible = false;
            _glideStartTime = 0;
        }

        // If you show the altimeter when the player has equipped elytra and
        // not on the ground, every little jump will flash the altimeter.
        // Test for gliding instead.
        if (_gaugesPossible && WingCommander.CONFIG.ALTIMETER_ENABLED && _showAltimeter) {
            double altitude = _player.getLocation().getY();
            _altitudeBossBar.setColor(WingCommander.CONFIG.getBarColor(WingCommander.CONFIG.ALTIMETER_COLOURS, (int) altitude));
            _altitudeBossBar.setTitle(String.format("Altitude: %d", (int) altitude));
            _altitudeBossBar.setProgress(Math.min(1.0, Math.max(0.0, altitude / WingCommander.CONFIG.ALTIMETER_CEILING)));
            _altitudeBossBar.setVisible(true);
        } else {
            _altitudeBossBar.setVisible(false);
        }

        if (_gaugesPossible && WingCommander.CONFIG.SPEEDOMETER_ENABLED && _showSpeedometer) {
            double speed = _player.getVelocity().length();
            _speedBossBar.setTitle(String.format("Speed: %3.1f", 20 * speed));
            _speedBossBar.setProgress(Math.min(1.0, Math.max(0.0, speed / WingCommander.CONFIG.SPEEDOMETER_MAX)));
            _speedBossBar.setVisible(true);
        } else {
            _speedBossBar.setVisible(false);
        }

        if (_gaugesPossible && WingCommander.CONFIG.WINGOMETER_ENABLED && _showWingometer) {
            ItemStack chest = _player.getEquipment().getChestplate();
            // On player death, chest item stack becomes null.
            if (chest != null) {
                int remainingDurability = Material.ELYTRA.getMaxDurability() - chest.getDurability();
                double fraction = remainingDurability / (double) Material.ELYTRA.getMaxDurability();
                int percentage = (int) (100 * fraction);
                _wingsBossBar.setColor(WingCommander.CONFIG.getBarColor(WingCommander.CONFIG.WINGOMETER_COLOURS, percentage));
                _wingsBossBar.setTitle(String.format("Wings: %d%%", percentage));
                _wingsBossBar.setProgress(Math.min(1.0, Math.max(0.0, fraction)));
                _wingsBossBar.setVisible(true);
            }
        } else {
            _wingsBossBar.setVisible(false);
        }

        if (_gaugesPossible && WingCommander.CONFIG.PITCHMETER_ENABLED && _showPitchmeter) {
            double pitch = -_player.getLocation().getPitch();
            double fraction = (pitch - WingCommander.CONFIG.PITCHMETER_MIN) /
                              (WingCommander.CONFIG.PITCHMETER_MAX - WingCommander.CONFIG.PITCHMETER_MIN);
            _pitchBossBar.setColor(WingCommander.CONFIG.getBarColor(WingCommander.CONFIG.PITCHMETER_COLOURS, (int) pitch));
            _pitchBossBar.setTitle(String.format("Pitch: %3.1f°", pitch));
            _pitchBossBar.setProgress(Math.min(1.0, Math.max(0.0, fraction)));
            _pitchBossBar.setVisible(true);
        } else {
            _pitchBossBar.setVisible(false);
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
        _gaugesPossible = true;
        Location loc = _player.getLocation();

        // Base the pitch on the current (not new) speed. Good enough.
        // Pitch is limited to the [0.5, 2.0] range in the client, apparently.
        // Dot product measures whether player is moving in the direction they
        // look.
        Vector velocity = _player.getVelocity();
        Vector look = _player.getLocation().getDirection();
        float speedFraction = (float) (Math.max(0.0, velocity.dot(look)) / WingCommander.CONFIG.MAX_VELOCITY);
        float pitch = 0.5f + 1.5f * speedFraction;

        // Method precondition is "wearing elytra irrespective of durability".
        // Check for unbroken elytra.
        if (WingCommander.isWearingElytra(_player, true)) {
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
        } else {
            loc.getWorld().playSound(loc, WingCommander.CONFIG.BROKEN_SOUND,
                                     WingCommander.CONFIG.BROKEN_VOLUME, pitch);
            if (WingCommander.CONFIG.BROKEN_GLIDE) {
                _player.setGliding(true);
                _player.setFallDistance(Math.max(0, _player.getFallDistance() - WingCommander.CONFIG.BROKEN_GLIDE_FALL_REDUCTION));
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
     * If true, gauges can be shown.
     *
     * In order to "debounce" gauge activation, this flag will not be set until
     * after the player has glided for at least
     * WingCommander.CONFIG.GAUGE_DEBOUNCE_MILLIS milliseconds.
     */
    protected boolean _gaugesPossible;

    /**
     * Start time of the player's most recent glide, if they have permission to
     * use gauges.
     */
    protected long _glideStartTime;

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
     * BossBar used to display the player's pitch.
     */
    protected BossBar _pitchBossBar;

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

    /**
     * If true, the pitch meter is visible (notwithstanding other requirements
     * for it to be shown).
     */
    protected boolean _showPitchmeter;

} // class PlayerState