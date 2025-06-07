package com.jouriroosjen.hardcoreSMPPlugin.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(JavaPlugin plugin) throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        connection = DriverManager.getConnection(url);
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
