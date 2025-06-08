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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Handles the /confirm command which allows players to confirm a pending buyback (revival).
 *
 * @author Jouri Roosjen
 * @version 0.3.0
 */
public class ConfirmCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final BuybackManager buybackManager;

    /**
     * Represents a buyback assist
     *
     * @param givingPlayer The players who's giving the assist
     * @param amount       The assisted amount
     */
    private record BuybackAssist(UUID givingPlayer, double amount) {
    }

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
            try {
                // Get latest deathId and corresponding assists
                OptionalInt correspondingDeathId = getLatestDeath(player.getUniqueId());
                List<BuybackAssist> allAssists = getAllAssists(correspondingDeathId.getAsInt());

                // Add all assists to piggy bank
                for (BuybackAssist assist : allAssists) {
                    addToPiggyBank(assist.givingPlayer(), assist.amount(), 1);
                }

                // Let the player pay remaining amount
                int buybackAmount = plugin.getConfig().getInt("piggy-bank-amounts.normal-death", 10);
                double totalAssistedAmount = getTotalAssistedAmount(allAssists);
                addToPiggyBank(player.getUniqueId(), buybackAmount - totalAssistedAmount, 0);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed adding buyback to piggy bank!");
                e.printStackTrace();

                player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));

                return false;
            }

            return revivePlayer(buyback.target());
        }

        try {
            // Calculate assist amount and check if that amount is still available.
            double assistAmount = calculateAssistAmount(buyback.percentage().getAsInt());

            // Get corresponding death and check if assist amount is still available.
            OptionalInt correspondingDeathId = getLatestDeath(buyback.target());
            double availableAmount = checkAmountAvailable(correspondingDeathId.getAsInt());

            // Return message if the wanted assist amount exceeds the still available amount.
            if (availableAmount < assistAmount) {
                player.sendMessage(Component.text("There's not that much left for this buyback!", NamedTextColor.RED));
                return true;
            }

            // Create assist.
            createBuybackAssist(player.getUniqueId(), buyback.target(), correspondingDeathId.getAsInt(), assistAmount);

            // Send confirm message and play a sound.
            String assistMessage = plugin.getConfig().getString("messages.assist-created", "You're assist is placed!");
            TextComponent messageComponent = Component.text()
                    .content(assistMessage)
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD)
                    .build();
            player.sendMessage(messageComponent);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed creating buyback assist!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));

            return false;
        }

        return true;
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
     * Calculate the amount to assist based on the percentage.
     *
     * @param assistPercentage The percentage to assist the buyback with.
     * @return The amount that is being assisted.
     */
    private double calculateAssistAmount(int assistPercentage) {
        int buybackAmount = plugin.getConfig().getInt("piggy-bank-amounts.normal-death", 10);
        return buybackAmount * (assistPercentage / 100.0);
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

    /**
     * Adds amount to piggy bank.
     *
     * @param playerUuid The player that has credited this amount
     * @param amount     The amount to be added to the bank
     * @param isAssist   Whether this transaction is from an assist or not
     * @throws SQLException If a database error occurs
     */
    private void addToPiggyBank(UUID playerUuid, double amount, int isAssist) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO piggy_bank (player_uuid, amount, is_assist)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, playerUuid.toString());
            statement.setDouble(2, amount);
            statement.setInt(3, isAssist);
            statement.execute();
        }
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
     * Add buyback assist to database.
     *
     * @param sender  The UUID of the sender.
     * @param target  The UUID of the receiving player.
     * @param deathId The ID of the corresponding death of the receiving player.
     * @param amount  The amount of the assist.
     * @throws SQLException If database error occurs.
     */
    private void createBuybackAssist(UUID sender, UUID target, int deathId, double amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO buyback_assists (giving_player_uuid, receiving_player_uuid, receiving_player_death_id, amount)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, sender.toString());
            statement.setString(2, target.toString());
            statement.setInt(3, deathId);
            statement.setDouble(4, amount);
            statement.execute();
        }
    }

    /**
     * Check the amount that is still able to be assisted with.
     *
     * @param deathId The corresponding deathId.
     * @return The amount that can still be assisted with.
     * @throws SQLException If an database error occurs.
     */
    private double checkAmountAvailable(int deathId) throws SQLException {
        int buybackAmount = plugin.getConfig().getInt("piggy-bank-amounts.normal-death", 10);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (amount) FROM buyback_assists
                WHERE receiving_player_death_id = ?
                """)) {
            statement.setInt(1, deathId);

            try (ResultSet resultSet = statement.executeQuery()) {
                double assistedAmount = 0;
                while (resultSet.next()) {
                    assistedAmount += resultSet.getDouble("amount");
                }
                return (buybackAmount / 2.0) - assistedAmount;
            }
        }
    }

    /**
     * Gets all assists for a given death
     *
     * @param deathId The corresponding deathId.
     * @return A list containing all the assists.
     * @throws SQLException If a database error occurs.
     */
    private List<BuybackAssist> getAllAssists(int deathId) throws SQLException {
        List<BuybackAssist> assists = new ArrayList<>();
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

                    assists.add(new BuybackAssist(givingPlayer, amount));
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
    private double getTotalAssistedAmount(List<BuybackAssist> assists) {
        double total = 0;
        for (BuybackAssist assist : assists) {
            total += assist.amount;
        }
        return total;
    }
}
