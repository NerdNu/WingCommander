package io.totemo.wingcommander;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
     */
    public PlayerState(Player player) {
        _player = player;
        _altitudeBossBar = Bukkit.getServer().createBossBar("Altitude", BarColor.BLUE, BarStyle.SEGMENTED_20);
        _altitudeBossBar.addPlayer(_player);
        _altitudeBossBar.setProgress(0);
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

        // TODO: Show boss bar for fuel gauge.

        // If a player loses glide in flight, let them glide again in the air
        // by pressing crouch.
        if (_player.isSneaking() && WingCommander.isFlightCapable(_player) && !_player.isOnGround()) {
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
     * Update BossBars according to the player's state and the configuration.
     */
    protected void updateBossBars() {
        if (WingCommander.CONFIG.ALTIMETER_ENABLED && _player.isGliding()) {
            if (!_altitudeBossBar.isVisible()) {
                _altitudeBossBar.setVisible(true);
            }
            double altitude = _player.getLocation().getY();
            for (Entry<Integer, BarColor> entry : WingCommander.CONFIG.ALTIMETER_COLOURS.entrySet()) {
                if (altitude < entry.getKey()) {
                    _altitudeBossBar.setColor(entry.getValue());
                    break;
                }
            }
            _altitudeBossBar.setTitle(String.format("Altitude: %d", (int) altitude));
            _altitudeBossBar.setProgress(Math.min(1.0, Math.max(0.0, altitude / WingCommander.CONFIG.ALTIMETER_CEILING)));
        } else {
            if (_altitudeBossBar.isVisible()) {
                _altitudeBossBar.setVisible(false);
            }
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
            _player.setRemainingAir(0);
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
} // class PlayerState