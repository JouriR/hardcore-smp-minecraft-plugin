package com.jouriroosjen.hardcoreSMPPlugin;

import com.jouriroosjen.hardcoreSMPPlugin.database.DatabaseManager;
import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerDeathListener;
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
            getLogger().severe("Failed to connect to database!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
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
