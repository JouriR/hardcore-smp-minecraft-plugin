package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handles entity damage events
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class EntityDamageListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

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
    @EventHandler
    public void onPlayerDamageReceived(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_RECEIVED, event.getFinalDamage());
    }

    /**
     * Event handler for player damage given events.
     *
     * @param event The entity damage event.
     */
    @EventHandler
    public void onPlayerDamageGiven(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.TOTAL_DAMAGE_GIVEN, event.getFinalDamage());
    }
}
