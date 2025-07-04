package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.UUID;

/**
 * Handles player item consume events
 *
 * @author Jouri Roosjen
 * @version 1.1.0
 */
public class PlayerItemConsumeListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerItemConsumeListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerItemConsumeListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player item consume events.
     *
     * @param event The player item consume event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        Material consumedType = event.getItem().getType();

        // Handle milk buckets
        if (consumedType == Material.MILK_BUCKET) {
            playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.MILK_CONSUMED, 1);
            return;
        }

        // Handle potions
        if (consumedType == Material.POTION) {
            playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.POTIONS_CONSUMED, 1);
            return;
        }

        // Handle general edible items
        if (consumedType.isEdible())
            playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.FOOD_CONSUMED, 1);
    }
}
