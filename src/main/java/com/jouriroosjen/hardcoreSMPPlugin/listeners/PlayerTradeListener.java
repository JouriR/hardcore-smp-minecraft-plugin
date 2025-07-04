package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles player trade events.
 *
 * @author Jouri Roosjen
 * @version 1.0.1
 */
public class PlayerTradeListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Construct a new {@code PlayerTradeListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance
     */
    public PlayerTradeListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player trade events.
     *
     * @param event The player trade event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrade(PlayerTradeEvent event) {
        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.TRADES, 1);
    }
}
