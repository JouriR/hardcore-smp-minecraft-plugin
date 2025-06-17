package com.jouriroosjen.hardcoreSMPPlugin.managers;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerStatisticsManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    public PlayerStatisticsManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public void incrementStatistic(UUID playerUuid, PlayerStatisticsEnum playerStatistic, int incrementValue) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_statistics (player_uuid, statistic_type, value)
                VALUES (?, ?, ?)
                ON CONFLICT (player_uuid, statistic_type)
                DO UPDATE SET
                    value = value + ?
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
