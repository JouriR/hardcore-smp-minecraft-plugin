package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final Connection connection;

    public PlayerJoinListener(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        String playerUsername = player.getName().trim();

        try {
            if (!checkPlayerFirstJoin(playerUuid)) return;
            savePlayerToDatabase(playerUuid, playerUsername);
        } catch (SQLException e) {
            TextComponent messageComponent = Component.text()
                    .content("Internal error!")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .build();
            player.kick(messageComponent);

            plugin.getLogger().severe("Failed to check OR save first join of: " + playerUsername);
            e.printStackTrace();
        }
    }

    private boolean checkPlayerFirstJoin(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT (uuid) FROM players WHERE uuid = ?")) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    private void savePlayerToDatabase(UUID playerUuid, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO players (uuid, username) VALUES (?, ?)")) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, username);
            statement.execute();
        }
    }
}
