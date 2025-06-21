package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.enums.HologramEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.BuybackManager;
import com.jouriroosjen.hardcoreSMPPlugin.managers.HologramManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.PlayerAvatarUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
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

import java.awt.*;
import java.awt.Color;
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
 * @version 1.0.0
 */
public class ConfirmCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final BuybackManager buybackManager;
    private final HologramManager hologramManager;

    /**
     * Represents a buyback assist
     *
     * @param givingPlayer The players who's giving the assist
     * @param amount       The assisted amount
     */
    public record BuybackAssist(UUID givingPlayer, double amount) {
    }

    /**
     * Constructs a new {@code ConfirmCommand} instance.
     *
     * @param plugin          The main plugin instance
     * @param connection      The SQL database connection
     * @param buybackManager  The BuybackManager that tracks pending buybacks
     * @param hologramManager The HologramManager instance
     */
    public ConfirmCommand(JavaPlugin plugin, Connection connection, BuybackManager buybackManager, HologramManager hologramManager) {
        this.plugin = plugin;
        this.connection = connection;
        this.buybackManager = buybackManager;
        this.hologramManager = hologramManager;
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
        int buybackAmount;
        try {
            buybackAmount = getBuybackPrice(buyback.target());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed getting buyback price!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
            return false;
        }

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
                double totalAssistedAmount = getTotalAssistedAmount(allAssists);
                addToPiggyBank(player.getUniqueId(), buybackAmount - totalAssistedAmount, 0);
                hologramManager.updateHologram(HologramEnum.PIGGY_BANK);
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
            double assistAmount = calculateAssistAmount(buyback.percentage().getAsInt(), buybackAmount);

            // Get corresponding death and check if assist amount is still available.
            OptionalInt correspondingDeathId = getLatestDeath(buyback.target());
            double availableAmount = checkAmountAvailable(correspondingDeathId.getAsInt(), buybackAmount);

            // Return message if the wanted assist amount exceeds the still available amount.
            if (availableAmount < assistAmount) {
                player.sendMessage(Component.text("There's not that much left for this buyback!", NamedTextColor.RED));
                return true;
            }

            // Create assist.
            createBuybackAssist(player.getUniqueId(), buyback.target(), correspondingDeathId.getAsInt(), assistAmount);
            hologramManager.updateHologram(HologramEnum.LATEST_ASSIST);

            // Send confirm message and play a sound.
            String assistMessage = plugin.getConfig().getString("messages.assist-created", "You're assist is placed!");
            TextComponent messageComponent = Component.text()
                    .content(assistMessage)
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD)
                    .build();
            player.sendMessage(messageComponent);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            // Get the target player
            Player targetPlayer = plugin.getServer().getPlayer(buyback.target());
            if (targetPlayer != null) {
                String assistReceivedMessage = plugin.getConfig().getString("messages.assist-received", "You've received a %percentage%% assist from %player%!")
                        .replace("%player%", player.getName())
                        .replace("%percentage%", String.valueOf(buyback.percentage().getAsInt()));

                // Send message in-game if target is online
                if (targetPlayer.isOnline()) {
                    TextComponent receivedMessageComponent = Component.text()
                            .content(assistReceivedMessage)
                            .color(NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                            .build();
                    targetPlayer.sendMessage(receivedMessageComponent);
                    targetPlayer.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                }

                // Send message via Discord if player is offline
                if (!targetPlayer.isOnline()) {
                    String targetDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(targetPlayer.getUniqueId());
                    User targetDiscordUser = DiscordSRV.getPlugin().getJda().getUserById(targetDiscordId);

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setThumbnail(PlayerAvatarUtil.getPlayerAvatarUrl(player, 50));
                    embed.setDescription(assistReceivedMessage);
                    embed.setColor(Color.CYAN);

                    targetDiscordUser.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(embed.build()))
                            .queue();
                }
            }
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
     * @param buybackAmount    The price of the buyback
     * @return The amount that is being assisted.
     */
    private double calculateAssistAmount(int assistPercentage, int buybackAmount) {
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
     * @param deathId      The corresponding deathId.
     * @param buybackPrice The price of the buyback
     * @return The amount that can still be assisted with.
     * @throws SQLException If an database error occurs.
     */
    private double checkAmountAvailable(int deathId, int buybackPrice) throws SQLException {
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
                return (buybackPrice / 2.0) - assistedAmount;
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
}
