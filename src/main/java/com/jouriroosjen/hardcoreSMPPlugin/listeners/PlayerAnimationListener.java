package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

/**
 * Handles player animation events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerAnimationListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerAnimationListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerAnimationListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player air punch event.
     *
     * @param event The player animate event.
     */
    @EventHandler
    public void onAirPunch(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

         /*
          Check if:
          - Player hits a block
          - Player hits an entity
         */
        Player player = event.getPlayer();
        if (player.getTargetBlockExact(5) != null || player.rayTraceEntities(5) != null) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.AIR_PUNCHES, 1);
    }
}
