package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Handles player join events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final PlaytimeManager playtimeManager;

    /**
     * Constructs a new {@code PlayerJoinListener} instance.
     *
     * @param plugin          The main plugin instance
     * @param connection      The active database connection
     * @param playtimeManager The playtime manager instance
     */
    public PlayerJoinListener(JavaPlugin plugin, Connection connection, PlaytimeManager playtimeManager) {
        this.plugin = plugin;
        this.connection = connection;
        this.playtimeManager = playtimeManager;
    }

    /**
     * Event handler for player join events. Checks if the player is joining for the first time
     * and saves them to the database if so.
     *
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        String playerUsername = player.getName().trim();

        try {
            if (checkPlayerFirstJoin(playerUuid)) {
                savePlayerToDatabase(playerUuid, playerUsername);
            }
        } catch (SQLException e) {
            TextComponent messageComponent = Component.text()
                    .content("Internal error!")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .build();
            player.kick(messageComponent);

            plugin.getLogger().severe("Failed to check OR save first join of: " + playerUsername);
            e.printStackTrace();
        }

        playtimeManager.startSession(playerUuid);
    }

    /**
     * Checks if the given player has joined the server before.
     *
     * @param playerUuid The UUID of the player to check
     * @return {@code true} if the player is joining for the first time, {@code false} otherwise
     * @throws SQLException If a database error occurs
     */
    private boolean checkPlayerFirstJoin(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT (uuid) FROM players WHERE uuid = ?")) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    /**
     * Saves the given player's UUID and username into the {@code players} table.
     *
     * @param playerUuid The UUID of the player
     * @param username   The player's in-game name
     * @throws SQLException If a database error occurs
     */
    private void savePlayerToDatabase(UUID playerUuid, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO players (uuid, username) VALUES (?, ?)")) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, username);
            statement.execute();
        }
    }
}
