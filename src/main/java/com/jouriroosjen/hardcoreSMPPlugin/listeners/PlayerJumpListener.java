package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Handles player jump events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerJumpListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerJumpListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerJumpListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player jump events.
     *
     * @param event The player jump event.
     */
    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        // Update statistic on player jump
        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.JUMPED, 1);
    }
}
