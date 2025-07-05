package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles block place events.
 *
 * @author Jouri Roosjen
 * @version 1.0.1
 */
public class BlockPlaceListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code BlockPlaceListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager}.
     */
    public BlockPlaceListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for block place events.
     *
     * @param event The block place event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.BLOCKS_PLACED, 1);
    }
}
