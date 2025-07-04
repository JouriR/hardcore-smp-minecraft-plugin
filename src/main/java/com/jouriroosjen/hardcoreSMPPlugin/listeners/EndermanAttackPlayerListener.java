package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent;
import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handle Enderman attack player events
 *
 * @author Jouri Roosjen
 * @version 1.0.1
 */
public class EndermanAttackPlayerListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code EndermanAttackPlayerListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EndermanAttackPlayerListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for Enderman attack player event.
     *
     * @param event The Enderman attack player event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEndermanAttackPlayer(EndermanAttackPlayerEvent event) {
        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.ENDERMAN_ATTACKS, 1);
    }
}
