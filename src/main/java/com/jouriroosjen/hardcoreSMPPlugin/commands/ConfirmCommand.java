package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.managers.BuybackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Handles the /confirm command which allows players to confirm a pending buyback (revival).
 *
 * @author Jouri Roosjen
 * @version 0.1.0
 */
public class ConfirmCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final BuybackManager buybackManager;

    /**
     * Constructs a new {@code ConfirmCommand} instance.
     *
     * @param plugin         The main plugin instance
     * @param connection     The SQL database connection
     * @param buybackManager The BuybackManager that tracks pending buybacks
     */
    public ConfirmCommand(JavaPlugin plugin, Connection connection, BuybackManager buybackManager) {
        this.plugin = plugin;
        this.connection = connection;
        this.buybackManager = buybackManager;
    }

    /**
     * Called when a player executes the /confirm command.
     * It checks whether the player has a pending buyback confirmation.
     * If so, it proceeds to revive the target player stored in that buyback.
     *
     * @param sender  The command sender
     * @param command The command executed
     * @param label   The command label used
     * @param args    The command arguments
     * @return {@code true} if the command was processed
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (!buybackManager.hasPending(player.getUniqueId())) {
            sender.sendMessage(Component.text("You have NO pending confirmation!", NamedTextColor.RED));
            return true;
        }

        BuybackManager.PendingBuyback buyback = buybackManager.confirm(player.getUniqueId());

        if (buyback.percentage() == null) {
            return revivePlayer(buyback.target());
        }

        return revivePlayer(buyback.target());
    }

    /**
     * Revives a player.
     * This method will:
     * <ul>
     *     <li>Update their status to alive in the database.</li>
     *     <li>Teleport them to the world spawn.</li>
     *     <li>Restore their game mode and play a sound.</li>
     *     <li>Broadcast a confirmation message.</li>
     * </ul>
     *
     * @param targetUuid The UUID of the player to revive.
     * @return {@code true} if the operation was processed (regardless of outcome), {@code false} if it failed fatally.
     */
    private boolean revivePlayer(UUID targetUuid) {
        Player player = plugin.getServer().getPlayer(targetUuid);

        try {
            // Update alive status in database
            updatePlayerAliveStatus(player.getUniqueId());

            // Teleport to world spawn
            World world = Bukkit.getWorlds().get(0);
            Location spawn = world.getSpawnLocation();
            player.teleport(spawn);

            // Play sound
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);

            // Change gamemode to survival
            player.setGameMode(GameMode.SURVIVAL);

            // Send confirm message
            String confirmMessage = plugin.getConfig().getString("messages.buy-back-success", "You've been revived!");
            TextComponent messageComponent = Component.text()
                    .content("[SERVER] ")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .append(Component.text(player.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" "))
                    .append(Component.text(confirmMessage, NamedTextColor.GREEN))
                    .build();
            plugin.getServer().broadcast(messageComponent);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed reviving player " + player.getName().trim() + "!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));

            return false;
        }

        return true;
    }

    /**
     * Updates the player's alive status in the database to indicate they are alive.
     *
     * @param playerUuid The UUID of the player to update
     * @throws SQLException If a database access error occurs
     */
    private void updatePlayerAliveStatus(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                   UPDATE players SET
                       is_alive = 1,
                       updated_at = datetime('now')
                   WHERE uuid = ?
                """)) {
            statement.setString(1, playerUuid.toString());
            statement.execute();
        }
    }
}
