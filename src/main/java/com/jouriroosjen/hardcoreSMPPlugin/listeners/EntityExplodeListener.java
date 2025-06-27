package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

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
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        Entity source = tnt.getSource();

        Player player = null;

        if (source instanceof Player) {
            player = (Player) source;
        } else if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            player = (Player) projectile.getShooter();
        }

        if (player == null) return;

        long destroyedBlocksCount = event.blockList().stream()
                .filter(block -> {
                    Material type = block.getType();

                    return type.isSolid() &&
                            type != Material.BEDROCK &&
                            type != Material.OBSIDIAN &&
                            !type.name().contains("AIR") &&
                            !type.name().contains("FIRE");
                })
                .count();

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
    }
}
