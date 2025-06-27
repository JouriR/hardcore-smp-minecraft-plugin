package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles creeper ignite events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class CreeperIgniteListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<UUID, UUID> creeperIgnitedBy = new HashMap<>();
    private final Map<UUID, Long> recentFlintAndSteelUse = new HashMap<>();
    private final Map<UUID, UUID> creeperTargeting = new HashMap<>();

    private static final long INTERACTION_TIMEOUT = 3000; // 3 seconds

    /**
     * Constructs a new {@code CreeperIgniteListener} instance.
     *
     * @param plugin                  The plugin instance for scheduling tasks.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public CreeperIgniteListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
        this.plugin = plugin;

        // Start a repeating task to check for ignited creepers
        startCreeperCheckTask();
    }

    /**
     * Event handler for player ignites creeper with flint & steel.
     *
     * @param event The player interact entity event.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Creeper creeper)) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.FLINT_AND_STEEL) {
            // Player used flint and steel on creeper
            creeperIgnitedBy.put(creeper.getUniqueId(), event.getPlayer().getUniqueId());
            recentFlintAndSteelUse.put(creeper.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * Event handler for creeper ignite event (only for flint and steel ignition).
     *
     * @param event The creeper ignite event.
     */
    @EventHandler
    public void onCreeperIgnite(CreeperIgniteEvent event) {
        Creeper creeper = event.getEntity();

        if (!creeperIgnitedBy.containsKey(creeper.getUniqueId())) {
            // Find the closest player for any other ignition causes
            Player closestPlayer = creeper.getWorld().getPlayers().stream()
                    .filter(player -> player.getLocation().distance(creeper.getLocation()) <= 8.0)
                    .min((p1, p2) -> Double.compare(
                            p1.getLocation().distance(creeper.getLocation()),
                            p2.getLocation().distance(creeper.getLocation())
                    ))
                    .orElse(null);

            if (closestPlayer != null) {
                creeperIgnitedBy.put(creeper.getUniqueId(), closestPlayer.getUniqueId());
            }
        }
    }

    /**
     * Event handler for entity targeting (when creeper targets a player).
     *
     * @param event The entity target event.
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        creeperTargeting.put(creeper.getUniqueId(), player.getUniqueId());
    }

    /**
     * Starts a repeating task to check for ignited creepers.
     */
    private void startCreeperCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check all tracked creepers to see if they've started igniting
                creeperTargeting.entrySet().removeIf(entry -> {
                    UUID creeperUuid = entry.getKey();
                    UUID playerUuid = entry.getValue();

                    // Find the creeper entity
                    Creeper creeper = null;
                    for (org.bukkit.World world : Bukkit.getWorlds()) {
                        for (org.bukkit.entity.Entity entity : world.getEntities()) {
                            if (entity.getUniqueId().equals(creeperUuid) && entity instanceof Creeper) {
                                creeper = (Creeper) entity;
                                break;
                            }
                        }
                        if (creeper != null) break;
                    }

                    if (creeper == null) return true;

                    // Check if creeper is ignited (fuse time > 0)
                    if (creeper.getFuseTicks() > 0) {
                        creeperIgnitedBy.put(creeperUuid, playerUuid);
                        return true;
                    }

                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks (0.1 seconds)
    }

    /**
     * Event handler for entity explode event.
     *
     * @param event The entity explode event.
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;

        UUID playerUuid = creeperIgnitedBy.get(creeper.getUniqueId());

        if (playerUuid == null) return;

        Player responsiblePlayer = Bukkit.getPlayer(playerUuid);

        if (responsiblePlayer == null) return;

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

        playerStatisticsManager.incrementStatistic(responsiblePlayer.getUniqueId(), PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);

        // Clean up tracking data
        creeperIgnitedBy.remove(creeper.getUniqueId());
        recentFlintAndSteelUse.remove(creeper.getUniqueId());
        creeperTargeting.remove(creeper.getUniqueId());
    }

    /**
     * Event handler for entity death event.
     *
     * @param event The entity death event.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            // Clean up if creeper dies without exploding
            creeperIgnitedBy.remove(creeper.getUniqueId());
            recentFlintAndSteelUse.remove(creeper.getUniqueId());
            creeperTargeting.remove(creeper.getUniqueId());
        }
    }
}
