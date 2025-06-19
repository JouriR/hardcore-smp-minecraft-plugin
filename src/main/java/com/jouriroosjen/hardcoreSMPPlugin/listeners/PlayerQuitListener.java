package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles player quit events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlayerQuitListener implements Listener {
    private final PlaytimeManager playtimeManager;

    /**
     * Constructs a new {@code PlayerQuitListener} instance.
     *
     * @param playtimeManager The {@code playtimeManager} instance.
     */
    public PlayerQuitListener(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
    }

    /**
     * Event handler for player quit events.
     *
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        playtimeManager.stopSession(playerUuid);
    }
}
