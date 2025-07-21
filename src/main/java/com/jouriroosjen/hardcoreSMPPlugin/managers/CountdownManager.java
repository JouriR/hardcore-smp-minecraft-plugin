package com.jouriroosjen.hardcoreSMPPlugin.managers;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages event countdowns.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class CountdownManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    private BukkitTask trackingTask;

    private final Map<Integer, CountdownData> activeCountdowns = new ConcurrentHashMap<>();
    
    /**
     * Represents a countdown.
     *
     * @param id       The countdown ID.
     * @param endTime  The countdown end time.
     * @param finished {@code true} if the countdown has ended, {@code false} otherwise.
     */
    private record CountdownData(int id, Instant endTime, boolean finished) {
        /**
         * Check if the countdown has finished.
         *
         * @return {@code true} if the countdown has ended, {@code false} otherwise.
         */
        public boolean isFinished() {
            return Instant.now().isAfter(endTime);
        }

        /**
         * Calculates how many seconds are left before the countdown ends.
         *
         * @return The amount of seconds remaining in the countdown.
         */
        public long getSecondsRemaining() {
            return Math.max(0, endTime.getEpochSecond() - Instant.now().getEpochSecond());
        }
    }

    /**
     * Constructs a new {@code CountdownManager} instance.
     *
     * @param plugin     The main plugin instance.
     * @param connection The active database connection.
     */
    public CountdownManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;

        loadCountdowns();
        startTracking();
    }

    /**
     * Create a countdown in the database.
     *
     * @param endTime The date and time the countdown should end.
     */
    public void createCountdown(String endTime) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO countdowns (end_at, created_at, updated_at)
                    VALUES (?, datetime('now'), datetime('now'))
                    """)) {
                statement.setString(1, endTime);
                statement.execute();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) return;

                    int countdownId = keys.getInt(1);

                    Instant endInstant = Instant.parse(endTime);
                    CountdownData countdown = new CountdownData(countdownId, endInstant, false);
                    activeCountdowns.put(countdownId, countdown);

                    plugin.getLogger().info("Countdown created with ID: " + countdownId);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Load all active countdowns from the database.
     */
    private void loadCountdowns() {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, end_at, finished
                    FROM countdowns
                    WHERE finished = 0
                    ORDER BY end_at
                    """)) {

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String endTimeString = resultSet.getString("end_at");
                        boolean finished = resultSet.getBoolean("finished");

                        try {
                            Instant endTime = Instant.parse(endTimeString);
                            CountdownData countdown = new CountdownData(id, endTime, finished);
                            activeCountdowns.put(id, countdown);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Start the countdown tracking task.
     */
    private void startTracking() {
        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (CountdownData countdown : activeCountdowns.values()) {
                    if (countdown.isFinished() && !countdown.finished())
                        handleCountdownFinished(countdown);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Handle when a countdown expires.
     *
     * @param countdown The expired countdown.
     */
    private void handleCountdownFinished(CountdownData countdown) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE countdowns
                    SET finished = 1, updated_at = datetime('now')
                    WHERE id = ?
                    """)) {
                statement.setInt(1, countdown.id());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        activeCountdowns.remove(countdown.id());

        plugin.getLogger().info("Countdown " + countdown.id() + " has finished!");
    }

    /**
     * Stop the countdown tracking and clean up resources.
     */
    public void shutdown() {
        if (trackingTask != null && !trackingTask.isCancelled())
            trackingTask.cancel();

        activeCountdowns.clear();
    }
}
