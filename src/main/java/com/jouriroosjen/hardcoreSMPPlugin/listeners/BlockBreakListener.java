package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles block break events
 *
 * @author Jouri Roosjen
 * @version 1.0.0
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
     * Event handler for potato harvest event.
     *
     * @param event The block break event.
     */
    @EventHandler
    public void onPotatoHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.POTATOES) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        Player player = event.getPlayer();
        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.POTATOES_HARVESTED, 1);
    }
}
