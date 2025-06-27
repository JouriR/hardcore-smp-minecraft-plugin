package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles block explode events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class BlockExplodeListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatisticsManager playerStatisticsManager;

    private final Map<Location, UUID> recentBedInteractions = new HashMap<>();
    private static final long BED_INTERACTION_TIMEOUT = 5000; // 5 seconds

    /**
     * Constructs a new {@code BlockExplodeListener} instance.
     *
     * @param plugin                  The main plugin instance.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public BlockExplodeListener(JavaPlugin plugin, PlayerStatisticsManager playerStatisticsManager) {
        this.plugin = plugin;
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for block explode events.
     *
     * @param event The block explode event.
     */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Block explodedBlock = event.getBlock();

        World.Environment environment = explodedBlock.getWorld().getEnvironment();
        if (environment != World.Environment.NETHER && environment != World.Environment.THE_END) return;

        // Since the bed block is already AIR when this event fires, we need to check if this explosion
        // was caused by a bed by looking for recent bed interactions at this location
        Player responsiblePlayer = findPlayerWhoUsedBed(explodedBlock);

        if (responsiblePlayer == null) return;

        long destroyedBlocksCount = event.blockList().stream()
                .filter(block -> {
                    Material type = block.getType();
                    return type.isSolid() &&
                            type != Material.BEDROCK &&
                            type != Material.OBSIDIAN &&
                            !type.isAir() &&
                            type != Material.FIRE &&
                            type != Material.SOUL_FIRE;
                })
                .count();

        playerStatisticsManager.incrementStatistic(responsiblePlayer.getUniqueId(), PlayerStatisticsEnum.BEDS_EXPLODED, 1);
        playerStatisticsManager.incrementStatistic(responsiblePlayer.getUniqueId(), PlayerStatisticsEnum.BLOCKS_DESTROYED, destroyedBlocksCount);
    }

    /**
     * Event handler for player interact events.
     *
     * @param event The player interact event.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material blockType = event.getClickedBlock().getType();
        if (!isBedBlock(blockType)) return;

        World.Environment environment = event.getClickedBlock().getWorld().getEnvironment();
        if (environment != World.Environment.NETHER && environment != World.Environment.THE_END) return;

        Location bedLocation = event.getClickedBlock().getLocation();
        UUID playerUuid = event.getPlayer().getUniqueId();

        // Store both the clicked block and its adjacent bed block
        recentBedInteractions.put(bedLocation, playerUuid);

        // Also store nearby bed blocks (since beds are 2 blocks)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;

                Location nearbyLocation = bedLocation.clone().add(x, 0, z);
                Block nearbyBlock = nearbyLocation.getBlock();

                if (isBedBlock(nearbyBlock.getType())) recentBedInteractions.put(nearbyLocation, playerUuid);
            }
        }

        // Clean up after timeout
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanupPlayerInteractions(playerUuid);
        }, BED_INTERACTION_TIMEOUT / 50);
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
     * Find the player that used the bed.
     *
     * @param bedBlock The bed that exploded.
     * @return The player that interacted with the bed.
     */
    private Player findPlayerWhoUsedBed(Block bedBlock) {
        Location bedLocation = bedBlock.getLocation();

        // Check exact location first
        UUID playerUuid = recentBedInteractions.get(bedLocation);
        if (playerUuid != null) {
            Player player = Bukkit.getPlayer(playerUuid);

            if (player != null) {
                cleanupPlayerInteractions(playerUuid);
                return player;
            }
        }

        // Check nearby locations (expanded search area)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue;

                Location checkLocation = bedLocation.clone().add(x, 0, z);
                playerUuid = recentBedInteractions.get(checkLocation);

                if (playerUuid != null) {
                    Player player = Bukkit.getPlayer(playerUuid);

                    if (player != null) {
                        cleanupPlayerInteractions(playerUuid);
                        return player;
                    }
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
}