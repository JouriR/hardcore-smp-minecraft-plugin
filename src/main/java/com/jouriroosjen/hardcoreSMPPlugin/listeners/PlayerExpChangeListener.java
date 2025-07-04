package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Handles player exp change events.
 *
 * @author Jouri Roosjen
 * @version 1.0.1
 */
public class PlayerExpChangeListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerExpChangeListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerExpChangeListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player exp change events.
     *
     * @param event The player exp change event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        int expAmount = event.getAmount();
        if (expAmount <= 0) return;

        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.TOTAL_EXPERIENCE_GAINED, expAmount);
    }
}
