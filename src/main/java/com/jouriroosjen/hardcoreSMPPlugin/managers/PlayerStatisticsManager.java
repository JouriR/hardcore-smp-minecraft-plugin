package com.jouriroosjen.hardcoreSMPPlugin.managers;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages the player statistics.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerStatisticsManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Constructs a new {@code PlayerStatisticsManager} instance.
     *
     * @param plugin     The main plugin instance.
     * @param connection The active database connection.
     */
    public PlayerStatisticsManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Increments a player statistic using the given amount.
     *
     * @param playerUuid      The UUID of the target player.
     * @param playerStatistic The statistic to update.
     * @param incrementValue  The value of which to increment the statistic with.
     */
    public void incrementStatistic(UUID playerUuid, PlayerStatisticsEnum playerStatistic, int incrementValue) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_statistics (player_uuid, statistic_type, value)
                VALUES (?, ?, ?)
                ON CONFLICT (player_uuid, statistic_type)
                DO UPDATE SET
                    value = value + ?,
                    updated_at = datetime('now')
                """)) {
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, playerStatistic.getId());
            statement.setInt(3, incrementValue);
            statement.setInt(4, incrementValue);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(
                    "Unable to increment statistic ( " + playerStatistic.getId() + " - " + incrementValue + " ) for player " + playerUuid.toString()
            );
            e.printStackTrace();
        }
    }
}
