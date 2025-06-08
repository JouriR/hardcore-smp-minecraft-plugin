package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.managers.BuybackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Command executor for the {@code /buyback} command, allowing dead players to revive themselves.
 *
 * @author Jouri Roosjen
 * @version 0.3.0
 */
public class BuyBackCommand implements CommandExecutor, TabExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final BuybackManager buybackManager;

    /**
     * Constructs a new {@code BuyBackCommand} instance.
     *
     * @param plugin         The main plugin instance
     * @param connection     The active database connection
     * @param buybackManager The active buyback manager
     */
    public BuyBackCommand(JavaPlugin plugin, Connection connection, BuybackManager buybackManager) {
        this.plugin = plugin;
        this.connection = connection;
        this.buybackManager = buybackManager;
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

            // Check if player has pending confirmations
            if (buybackManager.hasPending(player.getUniqueId())) {
                player.sendMessage(Component.text("You still have a pending confirmation!", NamedTextColor.RED));
                return true;
            }

            // Create pending confirmation
            buybackManager.addPending(player.getUniqueId(), player.getUniqueId(), null);
            player.sendMessage(
                    Component.text("Klik hier om je buyback te bevestigen! (Of gebruik /confirm)")
                            .color(NamedTextColor.YELLOW)
                            .decorate(TextDecoration.BOLD)
                            .decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.runCommand("/confirm"))
            );

            return true;
        }

        // Return false if there are not enough arguments for an assist
        if (args.length != 2) return false;

        String targetName = args[0];
        int percentage;

        // Get targeted player for assist
        Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return false;
        }

        // Check if target is not the sender
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

        // Check if target is really dead
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

        // Check if sender has pending confirmations
        if (buybackManager.hasPending(player.getUniqueId())) {
            player.sendMessage(Component.text("You still have a pending confirmation!", NamedTextColor.RED));
            return true;
        }

        // Create pending confirmation
        buybackManager.addPending(player.getUniqueId(), targetPlayer.getUniqueId(), OptionalInt.of(percentage));
        player.sendMessage(
                Component.text("Klik hier om je buyback assist te bevestigen! (Of gebruik /confirm)")
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/confirm"))
        );

        return true;
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
}
