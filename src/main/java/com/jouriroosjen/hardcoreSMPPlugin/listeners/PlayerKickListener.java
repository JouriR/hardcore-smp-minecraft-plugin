package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.managers.PlaytimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;

public class PlayerKickListener implements Listener {
    private final PlaytimeManager playtimeManager;

    public PlayerKickListener(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        playtimeManager.stopSession(playerUuid);
    }
}
