package com.jouriroosjen.hardcoreSMPPlugin.managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player playtime sessions and persists playtime data to the database.
 *
 * @author Jouri Roosjen
 * @version 0.1.0
 */
public class PlaytimeManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    private final Map<UUID, Long> sessionStartTimes = new HashMap<>();

    /**
     * Constructs a new {@code PlaytimeManager} instance.
     *
     * @param plugin     The main plugin instance
     * @param connection The active database connection
     */
    public PlaytimeManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Starts tracking the play session for a specific player.
     *
     * @param uuid The UUID of the player
     */
    public void startSession(UUID uuid) {
        sessionStartTimes.put(uuid, System.currentTimeMillis());
    }

    /**
     * Stops and clears all active player sessions.
     */
    public void stopAllSessions() {
        for (Map.Entry<UUID, Long> entry : sessionStartTimes.entrySet()) {
            stopSession(entry.getKey());
        }
    }

    /**
     * Stops a specific player's session.
     *
     * @param uuid The UUID of the player
     */
    public void stopSession(UUID uuid) {
        Long startTime = sessionStartTimes.remove(uuid);
        if (startTime == null) return;

        long endTime = System.currentTimeMillis();
        long elapsedTimeInSeconds = (endTime - startTime) / 1000;

        try {
            updatePlaytime(uuid, elapsedTimeInSeconds);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save playtime (" + elapsedTimeInSeconds + ") of: " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * Updates the total playtime of a player in the database by adding the given amount of time.
     *
     * @param uuid        The UUID of the player
     * @param elapsedTime The session duration to add, in seconds
     * @throws SQLException If a database error occurs
     */
    private void updatePlaytime(UUID uuid, long elapsedTime) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET playtime_seconds = playtime_seconds + ?
                WHERE uuid = ?
                """)) {
            statement.setLong(1, elapsedTime);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }
}
