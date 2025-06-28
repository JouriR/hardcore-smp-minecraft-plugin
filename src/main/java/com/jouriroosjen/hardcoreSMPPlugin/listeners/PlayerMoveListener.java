package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles player move events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerMoveListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

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
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            double distance = event.getFrom().distance(event.getTo());

            if (Double.isNaN(distance)) return;

            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.BLOCKS_TRAVELED, distance);
        }
    }
}
