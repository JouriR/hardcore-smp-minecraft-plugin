package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles block explode events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class BlockExplodeListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<Location, BedInteraction> recentBedInteractions = new ConcurrentHashMap<>();

    private static final long BED_INTERACTION_TIMEOUT = 5000L; // 5 seconds
    private static final long CLEANUP_INTERVAL = 10000L; // 10 seconds

    private static final Set<World.Environment> DANGEROUS_ENVIRONMENTS = EnumSet.of(
            World.Environment.NETHER,
            World.Environment.THE_END
    );

    /**
     * Represents a bed interaction.
     *
     * @param playerUuid The unique ID of the player that interacted.
     * @param timestamp  The timestamp when the interaction happened.
     */
    private record BedInteraction(UUID playerUuid, long timestamp) {
        /**
         * Checks if an interaction is expired or not.
         *
         * @param currentTime The current timestamp.
         * @return True if the interaction is expired, otherwise false.
         */
        boolean isExpired(long currentTime) {
            return currentTime - timestamp > BED_INTERACTION_TIMEOUT;
        }
    }

    /**
     * Constructs a new {@code BlockExplodeListener} instance.
     *
     * @param plugin                  The main plugin instance.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public BlockExplodeListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.plugin = plugin;
        this.playerStatisticsManager = playerStatisticsManager;

        startPeriodicCleanup();
    }

    /**
     * Event handler for block explode events.
     *
     * @param event The block explode event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Block explodedBlock = event.getBlock();
        World.Environment environment = explodedBlock.getWorld().getEnvironment();

        if (!DANGEROUS_ENVIRONMENTS.contains(environment)) return;

        // Since the bed block is already AIR when this event fires, we need to check if this explosion
        // was caused by a bed by looking for recent bed interactions at this location
        Player responsiblePlayer = findPlayerWhoUsedBed(explodedBlock.getLocation());
        if (responsiblePlayer == null) return;

        long destroyedBlocksCount = BlockUtil.countExplodableBlocks(event.blockList());

        UUID playerUuid = responsiblePlayer.getUniqueId();
        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.BEDS_EXPLODED, 1);

        if (destroyedBlocksCount > 0)
            playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
    }

    /**
     * Event handler for player interact events.
     *
     * @param event The player interact event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !isBedBlock(clickedBlock.getType())) return;

        World.Environment environment = clickedBlock.getWorld().getEnvironment();
        if (!DANGEROUS_ENVIRONMENTS.contains(environment)) return;

        UUID playerUuid = event.getPlayer().getUniqueId();
        Location bedLocation = clickedBlock.getLocation();
        long currentTime = System.currentTimeMillis();

        BedInteraction interaction = new BedInteraction(playerUuid, currentTime);
        recentBedInteractions.put(bedLocation, interaction);

        // Store adjacent bed block since beds are 2 blocks
        storeBedBlockInteractions(bedLocation, interaction);
    }

    /**
     * Check if a material is a bed block.
     *
     * @param material The material to check.
     * @return True if it's a bed block, otherwise false.
     */
    private boolean isBedBlock(Material material) {
        return material.name().endsWith("_BED");
    }

    /**
     * Store bed block interactions for adjacent blocks.
     *
     * @param bedLocation The bed location.
     * @param interaction The interaction.
     */
    private void storeBedBlockInteractions(Location bedLocation, BedInteraction interaction) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;

                Location nearbyLocation = bedLocation.clone().add(x, 0, z);
                Block nearbyBlock = nearbyLocation.getBlock();

                if (isBedBlock(nearbyBlock.getType()))
                    recentBedInteractions.put(nearbyLocation, interaction);
            }
        }
    }

    /**
     * Find the player that used the bed.
     *
     * @param explosionLocation The location of the explosion.
     * @return The player that interacted with the bed, or null if not found.
     */
    private Player findPlayerWhoUsedBed(Location explosionLocation) {
        long currentTime = System.currentTimeMillis();

        // Check exact location first
        BedInteraction interaction = recentBedInteractions.get(explosionLocation);
        if (interaction != null && !interaction.isExpired(currentTime)) {
            Player player = Bukkit.getPlayer(interaction.playerUuid);
            if (player == null) return null;

            cleanupPlayerInteractions(interaction.playerUuid);
            return player;
        }

        // Check nearby locations
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;

                Location checkLocation = explosionLocation.clone().add(x, 0, z);
                interaction = recentBedInteractions.get(checkLocation);

                if (interaction != null && !interaction.isExpired(currentTime)) {
                    Player player = Bukkit.getPlayer(interaction.playerUuid);
                    if (player == null) return null;

                    cleanupPlayerInteractions(interaction.playerUuid);
                    return player;
                }
            }
        }

        return null;
    }

    /**
     * Clean up all interactions for a specific player.
     *
     * @param playerUuid The player's UUID.
     */
    private void cleanupPlayerInteractions(UUID playerUuid) {
        recentBedInteractions.entrySet().removeIf(entry -> entry.getValue().equals(playerUuid));
    }

    /**
     * Start periodic cleanup task for expired interactions.
     */
    private void startPeriodicCleanup() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<Location, BedInteraction>> iterator = recentBedInteractions.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Location, BedInteraction> entry = iterator.next();

                if (entry.getValue().isExpired(currentTime)) iterator.remove();
            }
        }, CLEANUP_INTERVAL / 50, CLEANUP_INTERVAL / 50);
    }
}