package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles player teleport events
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerTeleportListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

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
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        switch (event.getCause()) {
            case CHORUS_FRUIT:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.CHORUS_FRUIT_CONSUMED, 1);
                break;

            case END_GATEWAY:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.END_GATEWAY_USED, 1);
                break;

            case END_PORTAL:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.END_PORTAL_USED, 1);
                break;

            case ENDER_PEARL:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.ENDER_PEARL_USED, 1);
                break;

            case NETHER_PORTAL:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.NETHER_PORTAL_USED, 1);
                break;

            case UNKNOWN:
                String fromWorldName = event.getFrom().getWorld().getName();
                String toWorldName = event.getTo().getWorld().getName();

                if (fromWorldName.equals("world_the_end") && toWorldName.equals("world")) {
                    playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.END_PORTAL_USED, 1);
                }
                break;

            default:
                break;
        }
    }
}
