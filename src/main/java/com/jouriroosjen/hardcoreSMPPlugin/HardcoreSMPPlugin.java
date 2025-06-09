package com.jouriroosjen.hardcoreSMPPlugin;

import com.jouriroosjen.hardcoreSMPPlugin.commands.BuyBackCommand;
import com.jouriroosjen.hardcoreSMPPlugin.commands.ConfirmCommand;
import com.jouriroosjen.hardcoreSMPPlugin.commands.PenalizeCommand;
import com.jouriroosjen.hardcoreSMPPlugin.database.DatabaseManager;
import com.jouriroosjen.hardcoreSMPPlugin.database.MigrationsManager;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerDeathListener;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerJoinListener;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerKickListener;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerQuitListener;
import com.jouriroosjen.hardcoreSMPPlugin.managers.BuybackManager;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
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
        playtimeManager = new PlaytimeManager(this, databaseManager.connection);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, databaseManager.connection), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, databaseManager.connection, playtimeManager), this);
        getServer().getPluginManager().registerEvents(new PlayerKickListener(playtimeManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(playtimeManager), this);

        // Register commands
        getCommand("buyback").setExecutor(new BuyBackCommand(this, databaseManager.connection, buybackManager));
        getCommand("confirm").setExecutor(new ConfirmCommand(this, databaseManager.connection, buybackManager));
        getCommand("penalize").setExecutor(new PenalizeCommand(this, databaseManager.connection));
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        // Clear managers
        buybackManager.clear();
        playtimeManager.stopAllSessions();

        // Close database connection
        try {
            if (databaseManager != null) databaseManager.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
