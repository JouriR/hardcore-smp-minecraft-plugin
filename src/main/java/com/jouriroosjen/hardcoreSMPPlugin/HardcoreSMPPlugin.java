package com.jouriroosjen.hardcoreSMPPlugin;

import com.jouriroosjen.hardcoreSMPPlugin.commands.BuyBackCommand;
import com.jouriroosjen.hardcoreSMPPlugin.database.DatabaseManager;
import com.jouriroosjen.hardcoreSMPPlugin.database.MigrationsManager;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerDeathListener;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class HardcoreSMPPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;

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

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, databaseManager.connection), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, databaseManager.connection), this);

        // Register commands
        getCommand("buyback").setExecutor(new BuyBackCommand(this, databaseManager.connection));
    }

    @Override
    public void onDisable() {
        // Close database connection
        try {
            if (databaseManager != null) databaseManager.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
