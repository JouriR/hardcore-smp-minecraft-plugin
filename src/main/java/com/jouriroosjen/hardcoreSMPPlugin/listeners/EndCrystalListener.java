package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles end crystal explosion tracking.
 *
 * @author Jouri Roosjen
 * @version 2.1.0
 */
public class EndCrystalListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<UUID, CrystalDamageData> crystalDamagers = new ConcurrentHashMap<>();

    private static final long DAMAGE_TIMEOUT = 5000L; // 5 seconds
    private static final long CLEANUP_INTERVAL = 10000L; // 10 seconds
    private static final double EXPLOSION_RADIUS = 12.0;

    /**
     * Represents crystal damage data.
     *
     * @param playerUuid The unique ID of the player damaging the crystal.
     * @param timestamp  The timestamp when the damage occurred.
     */
    private record CrystalDamageData(UUID playerUuid, long timestamp) {
        /**
         * Checks if the damage data is expired or not.
         *
         * @param currentTime The current timestamp.
         * @return True if the interaction is expired, otherwise false.
         */
        boolean isExpired(long currentTime) {
            return currentTime - timestamp > DAMAGE_TIMEOUT;
        }
    }

    /**
     * Constructs a new {@code EndCrystalListener} instance.
     *
     * @param plugin                  The main plugin instance.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EndCrystalListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.plugin = plugin;
        this.playerStatisticsManager = playerStatisticsManager;

        // Start periodic cleanup task
        startPeriodicCleanup();
    }

    /**
     * Event handler for entity damage by entity event.
     *
     * @param event The entity damage by entity event.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

        Player damager = getDamager(event);
        if (damager == null) return;

        CrystalDamageData damageData = new CrystalDamageData(damager.getUniqueId(), System.currentTimeMillis());
        crystalDamagers.put(crystal.getUniqueId(), damageData);
    }

    /**
     * Event handler for entity explode events.
     *
     * @param event The entity explode event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() != EntityType.END_CRYSTAL) return;

        UUID crystalUuid = event.getEntity().getUniqueId();
        CrystalDamageData damageData = crystalDamagers.remove(crystalUuid);

        if (damageData == null || damageData.isExpired(System.currentTimeMillis())) return;

        // Update blocks destroyed statistic
        if (!event.blockList().isEmpty()) {
            long destroyedBlocksCount = BlockUtil.countExplodableBlocks(event.blockList());

            if (destroyedBlocksCount > 0)
                playerStatisticsManager.incrementStatistic(damageData.playerUuid(), PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
        }

        Location explosionLocation = event.getLocation();

        // Update the total damage given statistic
        for (Entity entity : explosionLocation.getWorld().getNearbyEntities(explosionLocation, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != crystalUuid) {
                double distance = entity.getLocation().distance(explosionLocation);
                double damage = calculateCrystalDamage(distance);

                if (damage > 0)
                    playerStatisticsManager.incrementStatistic(damageData.playerUuid(), PlayerStatisticsEnum.TOTAL_DAMAGE_GIVEN, damage);
            }
        }
    }

    /**
     * Gets the player damager from the event.
     *
     * @param event The entity damage event.
     * @return The player damager, or null if not found.
     */
    private Player getDamager(EntityDamageByEntityEvent event) {
        // Direct player damage
        if (event.getDamager() instanceof Player player) return player;

        // Projectile damage
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }

        return null;
    }

    /**
     * Calculate the amount of damage done to an entity.
     *
     * @param distance The distance between the entity and the explosion.
     * @return The amount of damage done.
     */
    private double calculateCrystalDamage(double distance) {
        if (distance > EXPLOSION_RADIUS) return 0;

        double maxDamage = 99.0;
        double damageReduction = distance / EXPLOSION_RADIUS;
        return Math.max(0, maxDamage * (1.0 - damageReduction));
    }

    /**
     * Start periodic cleanup to remove expired damage data.
     */
    private void startPeriodicCleanup() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, CrystalDamageData>> iterator = crystalDamagers.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, CrystalDamageData> entry = iterator.next();
                if (entry.getValue().isExpired(currentTime)) iterator.remove();
            }
        }, CLEANUP_INTERVAL / 50, CLEANUP_INTERVAL / 50);
    }
}