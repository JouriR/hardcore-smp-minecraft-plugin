package com.jouriroosjen.hardcoreSMPPlugin.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages a SQLite database connection.
 * The database file is located inside the plugin's data folder and is named {@code database.db}.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class DatabaseManager {
    public final Connection connection;

    /**
     * Constructs a new {@code DatabaseManager} instance and connects to the SQLite database.
     *
     * @param plugin The main plugin instance
     * @throws SQLException If a database access error occurs or the connection fails
     */
    public DatabaseManager(JavaPlugin plugin) throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        connection = DriverManager.getConnection(url);
    }

    /**
     * Disconnects from the SQLite database if the connection is open.
     *
     * @throws SQLException If an error occurs while closing the connection
     */
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
