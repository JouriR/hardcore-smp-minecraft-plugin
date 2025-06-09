package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {
    private final PlaytimeManager playtimeManager;

    public PlayerQuitListener(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        playtimeManager.stopSession(playerUuid);
    }
}
