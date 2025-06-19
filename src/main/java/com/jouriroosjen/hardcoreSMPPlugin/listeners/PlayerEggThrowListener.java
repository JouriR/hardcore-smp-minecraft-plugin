package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEggThrowEvent;

/**
 * Handles player egg throw events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerEggThrowListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerEggThrowListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerEggThrowListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player egg throw events.
     *
     * @param event The player egg throw event.
     */
    @EventHandler
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        Player player = event.getPlayer();
        
        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.EGGS_THROWN, 1);

        // Check if the egg is hatching a chicken and update statistics
        if (event.isHatching() && event.getHatchingType() == EntityType.CHICKEN) {
            int hatchedAmount = event.getNumHatches();
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.CHICKENS_HATCHED, hatchedAmount);
        }
    }
}
