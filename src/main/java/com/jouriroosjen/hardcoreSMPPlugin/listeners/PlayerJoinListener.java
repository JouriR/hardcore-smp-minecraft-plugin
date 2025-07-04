package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles player join events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final PlaytimeManager playtimeManager;

    private final Set<UUID> playerCache = ConcurrentHashMap.newKeySet();

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

        initializePlayerCache();
    }

    /**
     * Event handler for player join events. Checks if the player is joining for the first time
     * and saves them to the database if so.
     *
     * @param event The player join event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        playtimeManager.startSession(playerUuid);

        if (playerCache.contains(playerUuid)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                handlePlayerFirstJoin(player);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Pre-populates the player cache with existing player UUIDs to improve performance.
     */
    private void initializePlayerCache() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM players")) {
                    ResultSet resultSet = statement.executeQuery();

                    while (resultSet.next()) {
                        try {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            playerCache.add(uuid);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID found in database: " +
                                    resultSet.getString("uuid"));
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to initialize player cache", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Handles the first-time join logic for a player.
     *
     * @param player The player who joined
     */
    private void handlePlayerFirstJoin(Player player) {
        UUID playerUuid = player.getUniqueId();
        String playerUsername = player.getName().trim();

        try {
            if (checkPlayerFirstJoin(playerUuid))
                savePlayerToDatabase(playerUuid, playerUsername);

            playerCache.add(playerUuid);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Database error while handling join for player: " + playerUsername, e);

            // Kick player on main thread if database error occurs
            new BukkitRunnable() {
                @Override
                public void run() {
                    Component errorMessage = Component.text()
                            .content("Database error! Please try again later.")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD)
                            .build();
                    player.kick(errorMessage);
                }
            }.runTask(plugin);
        }
    }

    /**
     * Checks if the given player has joined the server before.
     *
     * @param playerUuid The UUID of the player to check
     * @return {@code true} if the player is joining for the first time, {@code false} otherwise
     * @throws SQLException If a database error occurs
     */
    private boolean checkPlayerFirstJoin(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM players WHERE uuid = ? LIMIT 1")) {
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
