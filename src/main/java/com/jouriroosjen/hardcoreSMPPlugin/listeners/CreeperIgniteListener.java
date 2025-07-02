package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles creeper ignite events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class CreeperIgniteListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<UUID, IgnitionData> creeperIgnitionMap = new ConcurrentHashMap<>();

    private static final long INTERACTION_TIMEOUT = 3000L; // 3 seconds
    private static final long CLEANUP_INTERVAL = 5000L; // 5 seconds
    private static final double MAX_IGNITION_DISTANCE = 8.0;

    /**
     * Enum for different ignition causes.
     */
    private enum IgnitionCause {
        FLINT_AND_STEEL,
        PROXIMITY,
        TARGETING
    }

    /**
     * Represents creeper ignition data.
     *
     * @param playerUuid The unique ID of the player that caused the ignition.
     * @param cause      The ignition cause.
     * @param timestamp  The timestamp when this ignition occurred.
     */
    private record IgnitionData(UUID playerUuid, IgnitionCause cause, long timestamp) {
        /**
         * Constructs a new {@code IgnitionData} instance.
         *
         * @param playerUuid The unique ID of the player that caused the ignition.
         * @param cause      The ignition cause.
         */
        private IgnitionData(UUID playerUuid, IgnitionCause cause) {
            this(playerUuid, cause, System.currentTimeMillis());
        }

        /**
         * Checks if an interaction is expired or not.
         *
         * @param currentTime The current timestamp.
         * @return True if the interaction is expired, otherwise false.
         */
        boolean isExpired(long currentTime) {
            return currentTime - timestamp > INTERACTION_TIMEOUT;
        }
    }

    /**
     * Constructs a new {@code CreeperIgniteListener} instance.
     *
     * @param plugin                  The plugin instance for scheduling tasks.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public CreeperIgniteListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
        this.plugin = plugin;

        // Start periodic cleanup task
        startPeriodicCleanup();
    }

    /**
     * Event handler for player ignites creeper with flint & steel.
     *
     * @param event The player interact entity event.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Creeper creeper)) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() != Material.FLINT_AND_STEEL) return;

        // Store flint and steel ignition
        UUID playerUuid = event.getPlayer().getUniqueId();
        IgnitionData ignitionData = new IgnitionData(playerUuid, IgnitionCause.FLINT_AND_STEEL);
        creeperIgnitionMap.put(creeper.getUniqueId(), ignitionData);
    }

    /**
     * Event handler for creeper ignite event (only for flint and steel ignition).
     *
     * @param event The creeper ignite event.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreeperIgnite(CreeperIgniteEvent event) {
        Creeper creeper = event.getEntity();
        UUID creeperUuid = creeper.getUniqueId();

        if (creeperIgnitionMap.containsKey(creeperUuid)) return;

        Player closetsPlayer = findClosestPlayer(creeper);
        if (closetsPlayer == null) return;

        IgnitionData ignitionData = new IgnitionData(closetsPlayer.getUniqueId(), IgnitionCause.PROXIMITY);
        creeperIgnitionMap.put(creeperUuid, ignitionData);
    }

    /**
     * Event handler for entity targeting (when creeper targets a player).
     *
     * @param event The entity target event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        UUID creeperUuid = creeper.getUniqueId();
        if (creeperIgnitionMap.containsKey(creeperUuid)) return;

        IgnitionData ignitionData = new IgnitionData(player.getUniqueId(), IgnitionCause.TARGETING);
        creeperIgnitionMap.put(creeperUuid, ignitionData);
    }

    /**
     * Event handler for entity explode event.
     *
     * @param event The entity explode event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;

        UUID creeperUuid = creeper.getUniqueId();
        IgnitionData ignitionData = creeperIgnitionMap.remove(creeperUuid);
        if (ignitionData == null || ignitionData.isExpired(System.currentTimeMillis())) return;

        Player responsiblePlayer = Bukkit.getPlayer(ignitionData.playerUuid);
        if (responsiblePlayer == null) return;

        long destroyedBlocksCount = BlockUtil.countExplodableBlocks(event.blockList());

        UUID playerUuid = responsiblePlayer.getUniqueId();
        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.CREEPERS_IGNITED, 1);

        if (destroyedBlocksCount > 0)
            playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
    }

    /**
     * Event handler for entity death event.
     *
     * @param event The entity death event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        creeperIgnitionMap.remove(creeper.getUniqueId());
    }

    /**
     * Find the closest player to the given creeper.
     *
     * @param creeper The creeper.
     * @return The closest player, otherwise null if not found.
     */
    private Player findClosestPlayer(Creeper creeper) {
        Player closestPlayer = null;
        double closestDistance = MAX_IGNITION_DISTANCE;

        for (Player player : creeper.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(creeper.getLocation());
            if (distance <= closestDistance) {
                closestPlayer = player;
                closestDistance = distance;
            }
        }

        return closestPlayer;
    }

    /**
     * Start periodic cleanup to remove expired ignition data.
     */
    private void startPeriodicCleanup() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Iterator<Map.Entry<UUID, IgnitionData>> iterator = creeperIgnitionMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, IgnitionData> entry = iterator.next();
                if (entry.getValue().isExpired(System.currentTimeMillis())) iterator.remove();
            }
        }, CLEANUP_INTERVAL / 50, CLEANUP_INTERVAL / 50);
    }
}
