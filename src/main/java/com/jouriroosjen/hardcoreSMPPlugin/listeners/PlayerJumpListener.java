package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles player jump events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerJumpListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final ConcurrentHashMap<UUID, AtomicInteger> pendingJumps = new ConcurrentHashMap<>();

    private static final long BATCH_UPDATE_INTERVAL = 600L; // 30 seconds (600 ticks)
    private static final int MAX_CACHED_JUMPS = 400;

    /**
     * Constructs a new {@code PlayerJumpListener} instance.
     *
     * @param plugin                  The main plugin instance.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerJumpListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.plugin = plugin;
        this.playerStatisticsManager = playerStatisticsManager;

        // Start the batch update task
        startBatchUpdateTask();
    }

    /**
     * Event handler for player jump events.
     *
     * @param event The player jump event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJump(PlayerJumpEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();

        // Get or create atomic counter for this player
        AtomicInteger jumpCount = pendingJumps.computeIfAbsent(playerUuid, k -> new AtomicInteger(0));
        int newCount = jumpCount.incrementAndGet();

        // If too many jumps are cached, force an immediate update
        if (newCount >= MAX_CACHED_JUMPS)
            flushPlayerJumps(playerUuid, jumpCount);
    }

    /**
     * Starts the batch update task that saves cached jump counts to the database.
     */
    private void startBatchUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingJumps.isEmpty()) return;

                // Create a snapshot to avoid concurrent modification issues
                var snapshot = new ConcurrentHashMap<>(pendingJumps);

                for (Map.Entry<UUID, AtomicInteger> entry : snapshot.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    AtomicInteger jumpCount = entry.getValue();

                    if (jumpCount.get() <= 0) return;

                    flushPlayerJumps(playerUuid, jumpCount);
                }
            }
        }.runTaskTimerAsynchronously(plugin, BATCH_UPDATE_INTERVAL, BATCH_UPDATE_INTERVAL);
    }

    /**
     * Flushes the cached jump count for a specific player to the database.
     *
     * @param playerUuid The UUID of the player.
     * @param jumpCount  The atomic counter containing the jump count.
     */
    private void flushPlayerJumps(UUID playerUuid, AtomicInteger jumpCount) {
        int jumpsToFlush = jumpCount.getAndSet(0);

        if (jumpsToFlush <= 0) return;

        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.JUMPED, jumpsToFlush);
    }

    /**
     * Flush all pending jump counts to the database.
     */
    public void flushAllPendingJumps() {
        if (pendingJumps.isEmpty()) return;

        for (var entry : pendingJumps.entrySet()) {
            UUID playerUuid = entry.getKey();
            AtomicInteger jumpCount = entry.getValue();

            int jumps = jumpCount.get();
            if (jumps > 0)
                playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.JUMPED, jumps);
        }

        pendingJumps.clear();
    }
}
