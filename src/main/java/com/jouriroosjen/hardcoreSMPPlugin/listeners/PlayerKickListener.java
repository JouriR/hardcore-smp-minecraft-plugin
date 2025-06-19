package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;

/**
 * Handles player kick events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerKickListener implements Listener {
    private final PlaytimeManager playtimeManager;
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerKickListener} instance.
     *
     * @param playtimeManager         The {@code playtimeManager} instance.
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerKickListener(PlaytimeManager playtimeManager, PlayerStatisticsManager playerStatisticsManager) {
        this.playtimeManager = playtimeManager;
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player kick events.
     *
     * @param event The player kick event.
     */
    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        playtimeManager.stopSession(playerUuid);

        // Update statistic on idle kick
        if (event.getCause() == PlayerKickEvent.Cause.IDLING) {
            playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.IDLE_KICKS, 1);
        }
    }
}
