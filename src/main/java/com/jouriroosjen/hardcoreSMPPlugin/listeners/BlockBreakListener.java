package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles block break events
 *
 * @author Jouri Roosjen
 * @version 1.1.0
 */
public class BlockBreakListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code BlockBreakListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public BlockBreakListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for block break events.
     *
     * @param event The block break event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.BLOCKS_DESTROYED, 1);

        // Track if a potato is harvested
        if (block.getType() == Material.POTATOES &&
                block.getBlockData() instanceof Ageable ageable &&
                ageable.getAge() == ageable.getMaximumAge()) {
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.POTATOES_HARVESTED, 1);
        }
    }
}
