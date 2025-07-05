package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Set;

/**
 * Handles entity damage events
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class EntityDamageListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Set<BlockFace> ADJACENT_FACES = Set.of(
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    );

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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageReceived(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double damage = event.getFinalDamage();
        if (damage <= 0) return;

        PlayerStatisticsEnum specificDamageType = getSpecificDamageType(event, player);

        if (specificDamageType == null) {
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_RECEIVED, damage);
            return;
        }

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), specificDamageType, damage);
    }

    /**
     * Event handler for player damage given events.
     *
     * @param event The entity damage by entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageGiven(EntityDamageByEntityEvent event) {
        double damage = event.getFinalDamage();
        if (damage <= 0) return;

        Player damager = getPlayerFromDamageSource(event);
        if (damager == null) return;

        playerStatisticsManager.incrementStatistic(damager.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_GIVEN, damage);
    }

    /**
     * Gets the specific damage type for contact damage.
     *
     * @param event  The entity damage event.
     * @param player The player taking damage.
     * @return The damage type, or null if not found.
     */
    private PlayerStatisticsEnum getSpecificDamageType(EntityDamageEvent event, Player player) {
        if (event.getCause() != EntityDamageEvent.DamageCause.CONTACT) return null;

        Block playerBlock = player.getLocation().getBlock();

        for (BlockFace face : ADJACENT_FACES) {
            Material blockType = playerBlock.getRelative(face).getType();

            switch (blockType) {
                case CACTUS -> {
                    return PlayerStatisticsEnum.TOTAL_CACTUS_DAMAGE;
                }
                case SWEET_BERRY_BUSH -> {
                    return PlayerStatisticsEnum.TOTAL_BERRY_BUSH_DAMAGE;
                }
            }
        }

        return null;
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

        // Area Effect Cloud damage
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player) return (Player) source;

            return null;
        }

        // Firework damage
        if (damager instanceof Firework firework) {
            ProjectileSource shooter = firework.getShooter();
            if (shooter instanceof Player) return (Player) shooter;

            return null;
        }

        return null;
    }
}
