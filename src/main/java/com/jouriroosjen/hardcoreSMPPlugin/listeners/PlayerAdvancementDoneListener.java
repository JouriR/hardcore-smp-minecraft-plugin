package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * Handles player advancement done events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerAdvancementDoneListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerAdvancementDoneListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerAdvancementDoneListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player advancement done events.
     *
     * @param event The player advancement done event.
     */
    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.ADVANCEMENTS_DONE, 1);
    }
}
