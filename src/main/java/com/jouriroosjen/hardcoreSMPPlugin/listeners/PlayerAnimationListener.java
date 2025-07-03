package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.util.RayTraceResult;

/**
 * Handles player animation events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerAnimationListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    /**
     * Constructs a new {@code PlayerAnimationListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerAnimationListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player air punch event.
     *
     * @param event The player animate event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAirPunch(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();

        RayTraceResult result = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                5.0,
                org.bukkit.FluidCollisionMode.NEVER,
                true,
                0.0,
                entity -> entity != player
        );

        if (result != null) return;

        playerStatisticsManager.incrementStatistic(player.getUniqueId(), PlayerStatisticsEnum.AIR_PUNCHES, 1);
    }
}
