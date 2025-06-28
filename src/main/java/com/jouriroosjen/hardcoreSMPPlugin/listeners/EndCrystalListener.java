package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles end crystal explosion tracking.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class EndCrystalListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<UUID, UUID> crystalDamagers = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code EndCrystalListener} instance.
     *
     * @param plugin                  The main plugin instance.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public EndCrystalListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.plugin = plugin;
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for entity damage by entity event.
     *
     * @param event The entity damage by entity event.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) return;

        EnderCrystal crystal = (EnderCrystal) event.getEntity();
        Player damager = null;

        if (event.getDamager() instanceof Player) damager = (Player) event.getDamager();

        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof Player) damager = (Player) shooter;
        }

        if (damager != null) {
            crystalDamagers.put(crystal.getUniqueId(), damager.getUniqueId());
            plugin.getLogger().info("Player " + damager.getName() + " damaged end crystal at " + crystal.getLocation());
        }
    }

    /**
     * Event handler for entity explode events.
     *
     * @param event The entity explode event.
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() != EntityType.END_CRYSTAL) return;

        UUID crystalUuid = event.getEntity().getUniqueId();
        UUID playerUuid = crystalDamagers.remove(crystalUuid);

        if (playerUuid == null) {
            plugin.getLogger().info("End crystal exploded but no responsible player found");
            return;
        }

        Player responsiblePlayer = plugin.getServer().getPlayer(playerUuid);
        if (responsiblePlayer == null) {
            plugin.getLogger().info("Responsible player is no longer online");
            return;
        }

        long destroyedBlocksCount = event.blockList().stream()
                .filter(block -> {
                    return block.getType().isSolid() &&
                            block.getType() != Material.BEDROCK &&
                            block.getType() != Material.OBSIDIAN &&
                            !block.getType().name().contains("AIR") &&
                            !block.getType().name().contains("FIRE");
                })
                .count();

        plugin.getLogger().info("Player " + responsiblePlayer.getName() + " destroyed " + destroyedBlocksCount + " blocks with end crystal explosion at " + event.getLocation());
        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
    }
}