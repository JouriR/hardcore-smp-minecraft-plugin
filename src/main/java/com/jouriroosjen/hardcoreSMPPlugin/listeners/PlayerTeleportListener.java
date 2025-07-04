package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Handles player teleport events
 *
 * @author Jouri Roosjen
 * @version 1.1.0
 */
public class PlayerTeleportListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Map<PlayerTeleportEvent.TeleportCause, PlayerStatisticsEnum> TELEPORT_STATISTICS =
            new EnumMap<>(PlayerTeleportEvent.TeleportCause.class);

    static {
        TELEPORT_STATISTICS.put(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT, PlayerStatisticsEnum.CHORUS_FRUIT_CONSUMED);
        TELEPORT_STATISTICS.put(PlayerTeleportEvent.TeleportCause.END_GATEWAY, PlayerStatisticsEnum.END_GATEWAY_USED);
        TELEPORT_STATISTICS.put(PlayerTeleportEvent.TeleportCause.END_PORTAL, PlayerStatisticsEnum.END_PORTAL_USED);
        TELEPORT_STATISTICS.put(PlayerTeleportEvent.TeleportCause.ENDER_PEARL, PlayerStatisticsEnum.ENDER_PEARL_USED);
        TELEPORT_STATISTICS.put(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, PlayerStatisticsEnum.NETHER_PORTAL_USED);
    }

    /**
     * Constructs a new {@code PlayerTeleportListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerTeleportListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player teleport event.
     *
     * @param event The player teleport event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        PlayerStatisticsEnum statistic = TELEPORT_STATISTICS.get(cause);
        if (statistic != null) {
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), statistic, 1);
            return;
        }

        // Handle special case for UNKNOWN cause (end portal exit)
        if (cause == PlayerTeleportEvent.TeleportCause.UNKNOWN)
            handleUnknownTeleport(event, player);
    }

    /**
     * Handles teleport events with UNKNOWN cause, specifically end portal exits.
     *
     * @param event  The teleport event
     * @param player The player who teleported
     */
    private void handleUnknownTeleport(PlayerTeleportEvent event, Player player) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getWorld() == null || to == null || to.getWorld() == null)
            return;

        String fromWorldName = from.getWorld().getName();
        String toWorldName = to.getWorld().getName();

        // Detect end portal exit (end -> overworld)
        if ("world_the_end".equals(fromWorldName) && "world".equals(toWorldName))
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.END_PORTAL_USED, 1);
    }
}
