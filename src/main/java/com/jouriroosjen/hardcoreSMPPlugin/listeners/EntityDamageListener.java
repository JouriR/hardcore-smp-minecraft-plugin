package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Handles entity damage events
 *
 * @author Jouri Roosjen
 * @version 2.0.0
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
        Player damager = getPlayerFromDamageSource(event);
        if (damager == null) return;

        playerStatisticsManager.incrementStatistic(damager.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_GIVEN, event.getFinalDamage());
    }

    /**
     * Determines if damage is done by a player and returns that player.
     *
     * @param event The entity damage by entity event.
     * @return The player responsible for the damage, otherwise null
     */
    private Player getPlayerFromDamageSource(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // Direct damage
        if (damager instanceof Player) return (Player) damager;

        // Projectile damage
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) return (Player) shooter;

            return null;
        }

        // TNT damage
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player) return (Player) source;

            return null;
        }

        // Firework damage
        if (damager instanceof Firework firework) {
            ProjectileSource shooter = firework.getShooter();
            if (shooter instanceof Player) return (Player) shooter;

            return null;
        }

        // Area Effect Cloud damage
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player) return (Player) source;

            return null;
        }

        return null;
    }
}
