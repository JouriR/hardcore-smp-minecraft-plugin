package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player move events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerMoveListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<UUID, Location> lastPositions = new ConcurrentHashMap<>();

    private static final double MIN_DISTANCE = 0.1;

    /**
     * Constructs a new {@code PlayerMoveListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerMoveListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player move event.
     *
     * @param event The player move event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())
            return;

        Location lastPosition = lastPositions.get(playerUuid);
        if (lastPosition == null) {
            lastPositions.put(playerUuid, to.clone());
            return;
        }

        double distance = calculateDistance(lastPosition, to);

        if (distance < MIN_DISTANCE) return;

        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.BLOCKS_TRAVELED, distance);
        lastPositions.put(playerUuid, to.clone());
    }

    /**
     * Event handler for player quit events.
     *
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastPositions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Efficient distance calculation.
     *
     * @param from The last location of the player.
     * @param to   The new location of the player.
     * @return The traveled distance.
     */
    private double calculateDistance(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
