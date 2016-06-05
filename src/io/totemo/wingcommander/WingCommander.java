package io.totemo.wingcommander;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------
/**
 * Plugin, command handling and event handler class.
 */
public class WingCommander extends JavaPlugin implements Listener {
    /**
     * Configuration wrapper instance.
     */
    public static Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static WingCommander PLUGIN;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        CONFIG.reload();

        File playersFile = new File(WingCommander.PLUGIN.getDataFolder(), PLAYERS_FILE);
        _playerConfig = YamlConfiguration.loadConfiguration(playersFile);

        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    getState(player).onTick();
                }
            }
        }, 1, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        // Gauge settings weren't being saved on restart. Save all here.
        for (PlayerState state : _state.values()) {
            state.save(_playerConfig);
        }
        try {
            _playerConfig.save(new File(WingCommander.PLUGIN.getDataFolder(), PLAYERS_FILE));
        } catch (IOException ex) {
            WingCommander.PLUGIN.getLogger().warning("Unable to save player data: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase(getName())) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                CONFIG.reload();
                sender.sendMessage(ChatColor.GOLD + getName() + " configuration reloaded.");
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("gauge")) {
            cmdGauge(sender, args);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Invalid command syntax.");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * On join, allocate each player a {@link PlayerState} instance.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        _state.put(player.getName(), new PlayerState(player, _playerConfig));
    }

    // ------------------------------------------------------------------------
    /**
     * On quit, forget the {@link PlayerState}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerState state = _state.remove(event.getPlayer().getName());
        state.save(_playerConfig);
    }

    // ------------------------------------------------------------------------
    /**
     * Handle player crouch.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (isFlightCapable(player) && event.isSneaking()) {
            getState(player).onCrouch();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the player is wearing elytra.
     *
     * Note that this method doesn't care whether the elytra are broken
     * (durability <=1).
     *
     * @param player the player.
     * @return true if the player is wearing elytra.
     */
    protected static boolean isWearingElytra(Player player) {
        ItemStack chest = player.getEquipment().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the Player is wearing elytra and has permission for
     * powered flight.
     *
     * @param player the player.
     * @return true if the Player is wearing elytra and has permission for
     *         powered flight.
     */
    protected static boolean isFlightCapable(Player player) {
        ItemStack chest = player.getEquipment().getChestplate();
        return isWearingElytra(player) && player.hasPermission("wingcommander.fly");
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link PlayerState} for the specified player.
     *
     * @param player the player.
     * @return the {@link PlayerState} for the specified player.
     */
    protected PlayerState getState(Player player) {
        return _state.get(player.getName());
    }

    // ------------------------------------------------------------------------
    /**
     * Handle /gauge [altitude|speed] [off|on].
     *
     * Also accept /gauge help.
     */
    protected void cmdGauge(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You need to be in-game to use gauges.");
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help")) || args.length > 2) {
            sender.sendMessage(ChatColor.DARK_AQUA + "/gauge [altitude|speed|wings] [off|on]" +
                               ChatColor.WHITE + " - Toggle or set the visibility of a specific gauge or all gauges.");
        } else {
            String gauge;
            String visibilityString;
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("on")) {
                    gauge = null;
                    visibilityString = args[0];
                } else {
                    gauge = args[0];
                    visibilityString = null;
                }
            } else {
                gauge = args[0];
                visibilityString = args[1];
            }

            // Visibility null signifies toggle.
            Boolean visibility = null;
            if (visibilityString != null) {
                if (visibilityString.equalsIgnoreCase("off")) {
                    visibility = false;
                } else if (visibilityString.equalsIgnoreCase("on")) {
                    visibility = true;
                } else {
                    sender.sendMessage(ChatColor.RED + "The visibility value must be 'off' or 'on'.");
                    return;
                }
            }

            PlayerState state = getState((Player) sender);
            if (gauge == null) {
                state.showAltimeter(visibility);
                state.showSpeedometer(visibility);
                state.showWingometer(visibility);
                sender.sendMessage(ChatColor.DARK_AQUA + "All gauges will be " +
                                   (visibility ? "shown." : "hidden."));
            } else if (gauge.equalsIgnoreCase("altitude")) {
                state.showAltimeter(visibility);
                sender.sendMessage(ChatColor.DARK_AQUA + "The altimeter will be " +
                                   (state.isAltimeterShown() ? "shown." : "hidden."));
            } else if (gauge.equalsIgnoreCase("speed")) {
                state.showSpeedometer(visibility);
                sender.sendMessage(ChatColor.DARK_AQUA + "The speedometer will be " +
                                   (state.isSpeedometerShown() ? "shown." : "hidden."));
            } else if (gauge.equalsIgnoreCase("wings")) {
                state.showWingometer(visibility);
                sender.sendMessage(ChatColor.DARK_AQUA + "The wing durability meter will be " +
                                   (state.isWingometerShown() ? "shown." : "hidden."));
            } else {
                sender.sendMessage(ChatColor.RED + "The gauge name must be 'altitude' or 'speed'.");
            }
        }
    } // cmdGauge

    // ------------------------------------------------------------------------
    /**
     * Name of players file.
     */
    protected static final String PLAYERS_FILE = "players.yml";

    /**
     * Configuration file for per-player settings.
     */
    protected YamlConfiguration _playerConfig;

    /**
     * Map from Player name to {@link PlayerState} instance.
     *
     * A Player's PlayerState exists only for the duration of a login.
     */
    protected HashMap<String, PlayerState> _state = new HashMap<String, PlayerState>();
} // class WingCommander