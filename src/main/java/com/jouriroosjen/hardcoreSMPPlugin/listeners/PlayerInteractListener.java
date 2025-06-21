package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles player interact events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerInteractListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerInteractListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerInteractListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for when a player eats cake.
     *
     * @param event The player interact event
     */
    @EventHandler
    public void onPlayerEatCake(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getClickedBlock() == null ||
                event.getClickedBlock().getType() != Material.CAKE) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getFoodLevel() < 20)
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.CAKE_CONSUMED, 1);
    }
}
