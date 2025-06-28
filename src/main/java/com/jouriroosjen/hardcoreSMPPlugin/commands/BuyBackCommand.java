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
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Command executor for the {@code /buyback} command, allowing dead players to revive themselves.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class BuyBackCommand implements CommandExecutor {
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
            double price = calculatePrice(player, player.getUniqueId(), null);
            player.sendMessage(
                    Component.text("Deze buyback kost €" + price + " - Klik om te bevestigen! (Of gebruik /confirm)")
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
        double price = calculatePrice(player, targetPlayer.getUniqueId(), OptionalInt.of(percentage));
        player.sendMessage(
                Component.text("Deze assist kost €" + price + " - Klik om te bevestigen! (Of gebruik /confirm)")
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/confirm"))
        );

        return true;
    }

    /**
     * Calculate the price to be paid
     *
     * @param sender     The player using the command
     * @param playerUuid The target player
     * @param percentage The percentage to help with
     * @return The price or the buyback/assist
     */
    private double calculatePrice(Player sender, UUID playerUuid, OptionalInt percentage) {
        try {
            OptionalInt correspondingDeathId = getLatestDeath(playerUuid);
            List<ConfirmCommand.BuybackAssist> allAssists = getAllAssists(correspondingDeathId.getAsInt());
            double totalAssistedAmount = getTotalAssistedAmount(allAssists);
            int buybackPrice = getBuybackPrice(playerUuid);

            if (percentage == null) return buybackPrice - totalAssistedAmount;

            return calculateAssistAmount(percentage.getAsInt(), buybackPrice);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed calculating buyback price for " + playerUuid.toString() + "!");
            e.printStackTrace();

            sender.sendMessage(Component.text("Internal database error.", NamedTextColor.RED));
            return 0;
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
     * Get the current buyback price for the given player.
     *
     * @param playerUuid The player's UUID
     * @return The buyback price
     * @throws SQLException If a database error occurs
     */
    private int getBuybackPrice(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT (has_grace)
                        FROM players
                        WHERE uuid = ?
                """)) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getBoolean("has_grace")) {
                    return plugin.getConfig().getInt("piggy-bank-amounts.grace-period-death", 5);
                }
            }
        }

        return plugin.getConfig().getInt("piggy-bank-amounts.normal-death", 10);
    }

    /**
     * Get the ID of the player's last death.
     *
     * @param playerUuid The player of which the death ID should be.
     * @return An optional filled with the deathId if found, otherwise empty.
     * @throws SQLException If a database error occurs.
     */
    private OptionalInt getLatestDeath(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (id) FROM deaths
                WHERE player_uuid = ?
                ORDER BY datetime(created_at) DESC
                LIMIT 1
                """)) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return OptionalInt.of(resultSet.getInt("id"));
                }
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Gets all assists for a given death
     *
     * @param deathId The corresponding deathId.
     * @return A list containing all the assists.
     * @throws SQLException If a database error occurs.
     */
    private List<ConfirmCommand.BuybackAssist> getAllAssists(int deathId) throws SQLException {
        List<ConfirmCommand.BuybackAssist> assists = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT giving_player_uuid, amount
                FROM buyback_assists
                WHERE receiving_player_death_id = ?
                """)) {
            statement.setInt(1, deathId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID givingPlayer = UUID.fromString(resultSet.getString("giving_player_uuid"));
                    double amount = resultSet.getDouble("amount");

                    assists.add(new ConfirmCommand.BuybackAssist(givingPlayer, amount));
                }
            }
        }
        return assists;
    }

    /**
     * Get total assisted amount of the given assists list.
     *
     * @param assists A list containing all the assists to count up.
     * @return The total assisted amount.
     */
    private double getTotalAssistedAmount(List<ConfirmCommand.BuybackAssist> assists) {
        double total = 0;
        for (ConfirmCommand.BuybackAssist assist : assists) {
            total += assist.amount();
        }
        return total;
    }

    /**
     * Calculate the amount to assist based on the percentage.
     *
     * @param assistPercentage The percentage to assist the buyback with.
     * @param buybackAmount    The price of the buyback
     * @return The amount that is being assisted.
     */
    private double calculateAssistAmount(int assistPercentage, int buybackAmount) {
        return buybackAmount * (assistPercentage / 100.0);
    }
}
