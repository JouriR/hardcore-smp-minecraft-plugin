package com.jouriroosjen.hardcoreSMPPlugin;

import com.jouriroosjen.hardcoreSMPPlugin.commands.*;
import com.jouriroosjen.hardcoreSMPPlugin.database.DatabaseManager;
import com.jouriroosjen.hardcoreSMPPlugin.database.MigrationsManager;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.*;
import com.jouriroosjen.hardcoreSMPPlugin.managers.BuybackManager;
import com.jouriroosjen.hardcoreSMPPlugin.managers.HologramManager;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

/**
 * Main plugin class for HardcoreSMPPlugin.
 * <p>
 * Handles initialization and cleanup of all systems including database connections,
 * command registration, event listener binding, and manager setup.
 * </p>
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public final class HardcoreSMPPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private BuybackManager buybackManager;
    private HologramManager hologramManager;
    private PlayerStatisticsManager playerStatisticsManager;
    private PlaytimeManager playtimeManager;

    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();

        // Connect to database
        try {
            databaseManager = new DatabaseManager(this);
        } catch (SQLException e) {
            getLogger().severe("[DATABASE] Failed to connect to database!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        // Run database migration
        MigrationsManager migrationsManager = new MigrationsManager(this, databaseManager.connection);
        try {
            migrationsManager.migrate();
        } catch (SQLException e) {
            getLogger().severe("[DATABASE] Failed to migrate database!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        // Setup managers
        buybackManager = new BuybackManager(this);
        playerStatisticsManager = new PlayerStatisticsManager(this, databaseManager.connection);
        playtimeManager = new PlaytimeManager(this, databaseManager.connection);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new EndermanAttackPlayerListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new EntityBreedListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerAdvancementDoneListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerAnimationListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerBedEnterListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerEggThrowListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerExpChangeListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerFishListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerItemConsumeListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerItemDamageListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, databaseManager.connection, playtimeManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJumpListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerKickListener(playtimeManager, playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(playtimeManager), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleportListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleFlightListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSneakListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSprintListener(playerStatisticsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerTradeListener(playerStatisticsManager), this);

        // Register commands
        getCommand("buyback").setExecutor(new BuyBackCommand(this, databaseManager.connection, buybackManager));
        getCommand("my-debt").setExecutor(new MyDebtCommand(this, databaseManager.connection));

        // Delay hologram features registration until DecentHolograms is loaded
        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            initHologramFeatures();
        } else {
            getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onPluginEnable(PluginEnableEvent event) {
                    if (event.getPlugin().getName().equals("DecentHolograms")) {
                        initHologramFeatures();
                        HandlerList.unregisterAll(this);
                    }
                }
            }, this);
        }
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        // Clear managers
        buybackManager.clear();
        hologramManager.destroy();
        playtimeManager.stopAllSessions();
        playtimeManager.stopPlaytimeTracker();
        playtimeManager.stopPlaytimeBackupsTask();

        // Close database connection
        try {
            if (databaseManager != null) databaseManager.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize features that need the hologram manager
     */
    private void initHologramFeatures() {
        // Setup hologram manager
        hologramManager = new HologramManager(DecentHologramsAPI.get(), this, databaseManager.connection);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, databaseManager.connection, hologramManager), this);

        // Register commands
        getCommand("confirm").setExecutor(new ConfirmCommand(this, databaseManager.connection, buybackManager, hologramManager));
        getCommand("penalize").setExecutor(new PenalizeCommand(this, databaseManager.connection, hologramManager));
        getCommand("place-hologram").setExecutor(new PlaceHologramCommand(hologramManager));
    }
}
