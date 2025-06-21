package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Handles player item consume events
 *
 * @author Jouri Roosjen
 * @version 1.0.0
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
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material consumedType = event.getItem().getType();

        switch (consumedType) {
            case MILK_BUCKET:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.MILK_CONSUMED, 1);
                break;

            case POTION:
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.POTIONS_CONSUMED, 1);
                break;

            default:
                if (consumedType.isEdible())
                    playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.FOOD_CONSUMED, 1);
                break;
        }
    }
}
