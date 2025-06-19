package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Handles player item damage events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerItemDamageListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a ne {@code PlayerItemDamageListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerItemDamageListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player item damage event
     *
     * @param event The player item damage event.
     */
    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        int damageAmount = event.getDamage();

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_ITEM_DAMAGE, damageAmount);
    }
}
