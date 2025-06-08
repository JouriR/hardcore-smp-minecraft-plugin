package com.jouriroosjen.hardcoreSMPPlugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Command executor for the {@code /buyback} command, allowing dead players to revive themselves.
 *
 * @author Jouri Roosjen
 * @version 0.2.0
 */
public class BuyBackCommand implements CommandExecutor, TabExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Constructs a new {@code BuyBackCommand} instance.
     *
     * @param plugin     The main plugin instance
     * @param connection The active database connection
     */
    public BuyBackCommand(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Executes the buyback command logic when a player runs it.
     *
     * @param sender  The source of the command
     * @param command The command that was executed
     * @param label   The alias used
     * @param args    The command arguments
     * @return {@code true} if the command was handled, {@code false} otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            try {
                // Check if user is really dead
                if (!isPlayerDead(player.getUniqueId())) {
                    String notDeadMessage = plugin.getConfig().getString("messages.not-dead-error", "You are not dead!");
                    TextComponent messageComponent = Component.text()
                            .content(notDeadMessage)
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD)
                            .build();
                    player.sendMessage(messageComponent);
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed checking player " + player.getName().trim() + "!");
                e.printStackTrace();

                player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
                return false;
            }

            return revivePlayer(player);
        }

        if (args.length != 2) return false;

        String targetName = args[0];
        int percentage;

        Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return false;
        }

        if (targetPlayer == player) {
            player.sendMessage(Component.text("You cannot assist yourself!", NamedTextColor.RED));
            return false;
        }

        try {
            percentage = Integer.parseInt(args[1]);

            if (percentage < 1 || percentage > 50) {
                player.sendMessage(Component.text("Percentage must be between 1 and 50.", NamedTextColor.RED));
                return false;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Percentage must be a number!", NamedTextColor.RED));
            return false;
        }

        try {
            if (!isPlayerDead(targetPlayer.getUniqueId())) {
                String notDeadMessage = plugin.getConfig().getString("messages.assist-not-dead-error", "is not dead!");
                TextComponent messageComponent = Component.text()
                        .content(targetName)
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(" "))
                        .append(Component.text(notDeadMessage, NamedTextColor.RED))
                        .build();
                player.sendMessage(messageComponent);

                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed checking player " + targetName + "!");
            e.printStackTrace();
            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED));

            return false;
        }

        return revivePlayer(targetPlayer);
    }

    /**
     * Tab completer method (not used, yet).
     *
     * @param sender  The source of the command
     * @param command The command being executed
     * @param label   The command alias
     * @param args    The passed command arguments
     * @return List of suggestions or {@code null} if no suggestions
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return null;
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
     * @param player The player to revive.
     * @return {@code true} if the operation was processed (regardless of outcome), {@code false} if it failed fatally.
     */
    private boolean revivePlayer(Player player) {
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

            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed reviving player " + player.getName().trim() + "!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));

            return false;
        }
    }

    /**
     * Checks if the player is currently marked as dead in the database.
     *
     * @param playerUuid The UUID of the player
     * @return {@code true} if the player is dead, {@code false} if alive
     * @throws SQLException If a database access error occurs
     */
    private boolean isPlayerDead(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT is_alive FROM players WHERE uuid = ?")) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("is_alive") == 0;
            }
        }
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
