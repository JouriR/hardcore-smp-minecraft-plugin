package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Handles player fish events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerFishListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Construct a new {@code PlayerFishListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerFishListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player fish event.
     *
     * @param event The player fish event.
     */
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // Update statistic when player has caught a fish
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.FISH_CAUGHT, 1);
        }

        // Update statistic when player has caught the ground
        if (event.getState() == PlayerFishEvent.State.IN_GROUND) {
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.FISH_HOOK_IN_GROUND, 1);
        }

        // Update statistic when player has caught another player
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity hookedEntity = event.getHook().getHookedEntity();

            if (hookedEntity instanceof Player) {
                playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.FISH_HOOK_IN_PLAYER, 1);
            }
        }

        // Update statistic when player has failed to catch
        if (event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
            playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.FISHING_FAILED, 1);
        }
    }
}
