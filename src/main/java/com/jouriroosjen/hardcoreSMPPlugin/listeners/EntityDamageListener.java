package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles entity damage events
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class EntityDamageListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code EntityDamageListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EntityDamageListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player damage received events.
     *
     * @param event The entity damage event.
     */
    @EventHandler
    public void onPlayerDamageReceived(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean handled = false;

        if (event.getCause() == EntityDamageEvent.DamageCause.CONTACT) {
            Location loc = player.getLocation();
            World world = loc.getWorld();
            if (world == null) return;

            // Check surrounding blocks to see if touching cactus or berry bush
            for (BlockFace face : BlockFace.values()) {
                Block relative = player.getLocation().getBlock().getRelative(face);

                switch (relative.getType()) {
                    case CACTUS:
                        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_CACTUS_DAMAGE, event.getFinalDamage());
                        handled = true;
                        break;

                    case SWEET_BERRY_BUSH:
                        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_BERRY_BUSH_DAMAGE, event.getFinalDamage());
                        handled = true;
                        break;

                    default:
                        break;
                }
            }
        }

        if (!handled)
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_RECEIVED, event.getFinalDamage());
    }

    /**
     * Event handler for player damage given events.
     *
     * @param event The entity damage by entity event.
     */
    @EventHandler
    public void onPlayerDamageGiven(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_GIVEN, event.getFinalDamage());
    }
}
