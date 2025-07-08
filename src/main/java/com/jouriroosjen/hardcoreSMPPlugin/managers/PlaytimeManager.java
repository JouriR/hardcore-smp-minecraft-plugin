package com.jouriroosjen.hardcoreSMPPlugin.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player playtime sessions and persists playtime data to the database.
 *
 * @author Jouri Roosjen
 * @version 1.2.0
 */
public class PlaytimeManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    private final BukkitTask playtimeTracker;
    private final BukkitTask playtimeBackupsTask;

    private final Map<UUID, SessionData> sessionTimes = new ConcurrentHashMap<>();

    private record SessionData(Long originalStartTime, Long lastSaveTime) {
    }

    /**
     * Constructs a new {@code PlaytimeManager} instance.
     *
     * @param plugin     The main plugin instance
     * @param connection The active database connection
     */
    public PlaytimeManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;

        this.playtimeTracker = startPlaytimeTracker();
        this.playtimeBackupsTask = startPlaytimeBackupTask();
    }

    /**
     * Starts tracking the play session for a specific player.
     *
     * @param uuid The UUID of the player
     */
    public void startSession(UUID uuid) {
        Long now = System.currentTimeMillis();
        SessionData sessionData = new SessionData(now, now);

        sessionTimes.put(uuid, sessionData);
    }

    /**
     * Stops and clears all active player sessions.
     */
    public void stopAllSessions() {
        for (Map.Entry<UUID, SessionData> entry : sessionTimes.entrySet()) {
            stopSession(entry.getKey());
        }
    }

    /**
     * Stops a specific player's session.
     *
     * @param uuid The UUID of the player
     */
    public void stopSession(UUID uuid) {
        SessionData sessionData = sessionTimes.remove(uuid);
        if (sessionData == null) return;

        long endTime = System.currentTimeMillis();
        long totalElapsedTimeInSeconds = (endTime - sessionData.originalStartTime) / 1000;
        long timeSinceLastSave = (endTime - sessionData.lastSaveTime) / 1000;

        CompletableFuture.runAsync(() -> {
            try {
                updatePlaytime(uuid, timeSinceLastSave);
                addSessionToDatabase(uuid, totalElapsedTimeInSeconds);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to correctly handle session closure of: " + uuid);
                e.printStackTrace();
            }
        });
    }

    /**
     * Stops the playtime tracker
     */
    public void stopPlaytimeTracker() {
        playtimeTracker.cancel();
    }

    /**
     * Stops the playtime backup task
     */
    public void stopPlaytimeBackupsTask() {
        playtimeBackupsTask.cancel();
    }

    /**
     * Start a playtime tracker that runs every minute and check grace and minimum playtime status
     *
     * @return The BukkitTask that's started
     */
    private BukkitTask startPlaytimeTracker() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                int graceTimeInSeconds = plugin.getConfig().getInt("timings.grace-period", 7200);
                int minimumPlaytimeInSeconds = plugin.getConfig().getInt("timings.minimum-playtime", 90000);

                for (Map.Entry<UUID, SessionData> entry : sessionTimes.entrySet()) {
                    UUID uuid = entry.getKey();
                    SessionData sessionData = entry.getValue();
                    if (sessionData == null) continue;

                    long now = System.currentTimeMillis();
                    long elapsedTimeInSeconds = (now - sessionData.lastSaveTime) / 1000;

                    CompletableFuture.runAsync(() -> {
                        try {
                            long databasePlaytime = getUserPlaytimeFromDatabase(uuid);
                            long totalPlaytimeInSeconds = databasePlaytime + elapsedTimeInSeconds;
                            String playerName = plugin.getServer().getPlayer(uuid).getName();

                            // Check player grace status
                            if (getUserHasGrace(uuid) && totalPlaytimeInSeconds > graceTimeInSeconds) {
                                disableUserGrace(uuid);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        String graceOverMessage = plugin.getConfig().getString("messages.grace-over", "%player% is no longer protected and pays in full now!")
                                                .replace("%player%", playerName);

                                        Component messageComponent = Component.text("[SERVER] ")
                                                .color(NamedTextColor.GOLD)
                                                .decorate(TextDecoration.BOLD)
                                                .append(Component.text(graceOverMessage));

                                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                                            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 1);
                                        }
                                        plugin.getServer().broadcast(messageComponent);
                                    }
                                }.runTask(plugin);
                            }

                            // Check player minimum playtime
                            if (!getUserHasMinimumPlaytime(uuid) && totalPlaytimeInSeconds > minimumPlaytimeInSeconds) {
                                setUserReachedMinimumPlaytime(uuid);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        String graceOverMessage = plugin.getConfig().getString("messages.minimum-playtime-reached", "%player% has reached the minimum playtime!")
                                                .replace("%player%", playerName);

                                        Component messageComponent = Component.text("[SERVER] ")
                                                .color(NamedTextColor.GREEN)
                                                .decorate(TextDecoration.BOLD)
                                                .append(Component.text(graceOverMessage));

                                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                                            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 1);
                                        }
                                        plugin.getServer().broadcast(messageComponent);
                                    }
                                }.runTask(plugin);
                            }
                        } catch (SQLException e) {
                            plugin.getLogger().severe("Failed to get playtime for: " + uuid);
                            e.printStackTrace();
                        }
                    });
                }
            }
        }.runTaskTimer(plugin, 20L, 1200L); // Runs every 1200 tick (1 minute)
    }

    /**
     * Start a playtime backup task that runs every 5 minutes.
     * It updates the player's playtime in the database in case of a crash.
     *
     * @return The BukkitTask that's started
     */
    private BukkitTask startPlaytimeBackupTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, SessionData> entry : sessionTimes.entrySet()) {
                    UUID uuid = entry.getKey();
                    SessionData sessionData = entry.getValue();
                    if (sessionData == null) continue;

                    long now = System.currentTimeMillis();
                    long elapsedTimeInSeconds = (now - sessionData.lastSaveTime) / 1000;

                    try {
                        updatePlaytime(uuid, elapsedTimeInSeconds);
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Failed to update playtime (" + elapsedTimeInSeconds + ") for: " + uuid);
                        e.printStackTrace();
                    }

                    SessionData newSessionData = new SessionData(sessionData.originalStartTime, now);
                    entry.setValue(newSessionData);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 6000L); // Runs every 6000 tick (5 minutes) asynchronously
    }

    /**
     * Give the specified user a death grace period.
     *
     * @param playerUuid The player to give the death grace.
     */
    public void grantDeathGrace(UUID playerUuid) {
        try {
            setUserDeathGrace(playerUuid, true);

            int deathGraceTimeInSeconds = plugin.getConfig().getInt("timings.death-grace-period", 600);

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        setUserDeathGrace(playerUuid, false);

                        Player player = plugin.getServer().getPlayer(playerUuid);
                        if (!player.isConnected()) return;

                        Component messageComponent = Component.text("[SERVER] Je death grace is over!")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD);

                        player.sendMessage(messageComponent);
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Failed to remove death grace for: " + playerUuid);
                        e.printStackTrace();
                    }
                }
            }.runTaskLater(plugin, deathGraceTimeInSeconds * 20L);

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to grant death grace: " + playerUuid);
            e.printStackTrace();
        }
    }

    /**
     * Gets the player's playtime stored in the database.
     *
     * @param uuid The player's UUID
     * @return The playtime in seconds
     * @throws SQLException If a database error occurs
     */
    private long getUserPlaytimeFromDatabase(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (playtime_seconds)
                FROM players
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getLong("playtime_seconds");
            }
        }
        return 0;
    }

    /**
     * Gets the player's grace status from the database
     *
     * @param uuid The player's UUID
     * @return {@code true} if user still has grace, {@code false} otherwise
     * @throws SQLException If a database error occurs
     */
    private boolean getUserHasGrace(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (has_grace)
                FROM players
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getBoolean("has_grace");
            }
        }
        return false;
    }

    /**
     * Gets the player's minimum playtime status from the database
     *
     * @param uuid The player's UUID
     * @return {@code true} if user has reached the minimum playtime, {@code false} otherwise
     * @throws SQLException If a database error occurs
     */
    private boolean getUserHasMinimumPlaytime(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (has_minimum_playtime)
                FROM players
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getBoolean("has_minimum_playtime");
            }
        }
        return false;
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

    /**
     * Updates the grace status of a player to false.
     *
     * @param uuid The UUID of the player
     * @throws SQLException If a database error occurs
     */
    private void disableUserGrace(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET has_grace = 0
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    /**
     * Updates the minimum playtime status of a player to true.
     *
     * @param uuid The UUID of the player
     * @throws SQLException If a database error occurs
     */
    private void setUserReachedMinimumPlaytime(UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET has_minimum_playtime = 1
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    /**
     * Inserts a players session in the sessions table
     *
     * @param uuid                 The player's UUID
     * @param elapsedTimeInSeconds The session time in seconds
     * @throws SQLException If a database error occurs
     */
    private void addSessionToDatabase(UUID uuid, long elapsedTimeInSeconds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO sessions (player_uuid, playtime_seconds)
                        VALUES (?,?)
                """)) {
            statement.setString(1, uuid.toString());
            statement.setLong(2, elapsedTimeInSeconds);
            statement.execute();
        }
    }

    /**
     * Sets a player's death grace.
     *
     * @param uuid  The unique ID of the player to update.
     * @param value The value to set.
     * @throws SQLException If a database error occurs.
     */
    private void setUserDeathGrace(UUID uuid, boolean value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET has_death_grace = ?
                WHERE uuid = ?
                """)) {
            statement.setBoolean(1, value);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }
}
