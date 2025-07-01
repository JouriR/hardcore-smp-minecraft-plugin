package com.jouriroosjen.hardcoreSMPPlugin.managers;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the player statistics.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerStatisticsManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    private final AtomicBoolean isRunning;
    private final AtomicBoolean isShuttingDown;

    private final AtomicLong processedJobs;
    private final AtomicLong failedJobs;

    private final BlockingQueue<StatisticJob> jobQueue;
    private final Map<StatisticKey, Double> batchedIncrements;

    // Processing settings
    private static final int BATCH_SIZE = 50;
    private static final long BATCH_PROCESS_INTERVAL = 20L;
    private static final int MAX_QUEUE_SIZE = 10000;

    /**
     * Represents a statistic update job.
     *
     * @param playerUuid The unique ID of the player this job belongs to.
     * @param statistic  The statistic that should be updated.
     * @param value      The value to update the statistic with.
     */
    private record StatisticJob(UUID playerUuid, PlayerStatisticsEnum statistic, double value) {
    }

    /**
     * Represents a statistic batch key.
     *
     * @param playerUuid The unique ID of the player to find in the batch.
     * @param statistic  The statistic to find in the batch.
     */
    private record StatisticKey(UUID playerUuid, PlayerStatisticsEnum statistic) {
    }

    /**
     * Constructs a new {@code PlayerStatisticsManager} instance.
     *
     * @param plugin     The main plugin instance.
     * @param connection The active database connection.
     */
    public PlayerStatisticsManager(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;

        this.isRunning = new AtomicBoolean(false);
        this.isShuttingDown = new AtomicBoolean(false);

        this.processedJobs = new AtomicLong(0);
        this.failedJobs = new AtomicLong(0);

        this.jobQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.batchedIncrements = new ConcurrentHashMap<>();

        startWorkerThread();
        startBatchProcessor();
    }

    /**
     * Increments a player statistic using the given amount.
     *
     * @param playerUuid      The UUID of the target player.
     * @param playerStatistic The statistic to update.
     * @param incrementValue  The value of which to increment the statistic with.
     */
    public void incrementStatistic(UUID playerUuid, PlayerStatisticsEnum playerStatistic, double incrementValue) {
        if (isShuttingDown.get()) {
            plugin.getLogger().warning("Cannot queue statistic increment because the manager is shutting down!");
            return;
        }

        if (playerUuid == null || playerStatistic == null || Double.isNaN(incrementValue) || Double.isInfinite(incrementValue))
            return;

        StatisticKey key = new StatisticKey(playerUuid, playerStatistic);

        batchedIncrements.merge(key, incrementValue, Double::sum);
    }

    /**
     * Starts a new thread for the statistic job queue.
     */
    private void startWorkerThread() {
        new BukkitRunnable() {
            @Override
            public void run() {
                isRunning.set(true);

                while (!isShuttingDown.get() || !jobQueue.isEmpty()) {
                    try {
                        int processed = 0;

                        while (processed < BATCH_SIZE && !jobQueue.isEmpty()) {
                            StatisticJob job = jobQueue.poll();

                            if (job != null) {
                                processJob(job);
                                processed++;
                            }
                        }

                        if (processed == 0) {
                            Thread.sleep(50);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error in statistics worker thread: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                isRunning.set(false);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Starts a new thread for the increments batch processor.
     */
    private void startBatchProcessor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processBatchedIncrements();
            }
        }.runTaskTimerAsynchronously(plugin, BATCH_PROCESS_INTERVAL, BATCH_PROCESS_INTERVAL);
    }

    /**
     * Processes the batched increments.
     */
    private void processBatchedIncrements() {
        if (batchedIncrements.isEmpty()) return;

        Map<StatisticKey, Double> toProcess = new ConcurrentHashMap<>(batchedIncrements);
        batchedIncrements.clear();

        for (Map.Entry<StatisticKey, Double> entry : toProcess.entrySet()) {
            StatisticKey key = entry.getKey();
            Double value = entry.getValue();

            StatisticJob job = new StatisticJob(key.playerUuid, key.statistic, value);
            queueJob(job);
        }
    }

    /**
     * Add a new statistic job to the queue.
     *
     * @param job The statistic job to add.
     */
    private void queueJob(StatisticJob job) {
        if (!jobQueue.offer(job)) {
            failedJobs.incrementAndGet();
            plugin.getLogger().warning("Statistics job queue is full! Dropping job: " + job);
        }
    }

    /**
     * Process a statistic job.
     *
     * @param job The statistic job to process.
     */
    private void processJob(StatisticJob job) {
        try {
            executeIncrement(job.playerUuid, job.statistic, job.value);
            processedJobs.incrementAndGet();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process statistic job: " + job + " - " + e.getMessage());
            failedJobs.incrementAndGet();
        }
    }

    /**
     * Executes an increment job.
     *
     * @param playerUuid      The unique ID of the player whom's statistic should be incremented.
     * @param playerStatistic The statistic to increment.
     * @param value           The value to increment with.
     * @throws SQLException If a database error occurs.
     */
    private void executeIncrement(UUID playerUuid, PlayerStatisticsEnum playerStatistic, double value) throws SQLException {
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
            statement.setDouble(3, value);
            statement.setDouble(4, value);
            statement.executeUpdate();
        }
    }

    /**
     * Gracefully shutdown the manager and it's threads.
     */
    public void shutdown() {
        isShuttingDown.set(true);

        processBatchedIncrements();

        while (isRunning.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
