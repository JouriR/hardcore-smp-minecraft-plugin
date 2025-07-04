package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.BlockUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

/**
 * Handles entity explode events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class EntityExplodeListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code EntityExplodeListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EntityExplodeListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for entity explode events.
     *
     * @param event The entity explode event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        List<Block> blocks = event.blockList();
        if (blocks.isEmpty()) return;

        Player responsiblePlayer = getResponsiblePLayer(tnt);
        if (responsiblePlayer == null) return;

        long destroyedBlocksCount = BlockUtil.countExplodableBlocks(blocks);

        if (destroyedBlocksCount > 0)
            playerStatisticsManager.incrementStatistic(responsiblePlayer.getUniqueId(), PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
    }

    /**
     * Gets the player responsible for TNT ignition.
     *
     * @param tnt The ignited TNT entity.
     * @return The player responsible, otherwise null if not found.
     */
    private Player getResponsiblePLayer(TNTPrimed tnt) {
        Entity source = tnt.getSource();

        if (source instanceof Player player) return player;

        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter)
            return shooter;

        return null;
    }
}
