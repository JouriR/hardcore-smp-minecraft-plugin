package com.jouriroosjen.hardcoreSMPPlugin.commands;

import com.jouriroosjen.hardcoreSMPPlugin.enums.HologramEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.HologramManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.PlayerAvatarUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * A command executor for the `/penalize` command.
 * This command allows server operators (OPs) to issue a monetary penalty to a player for a given reason.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PenalizeCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final HologramManager hologramManager;

    /**
     * Constructs a new {@code PenalizeCommand} instance.
     *
     * @param plugin          The main plugin instance
     * @param connection      The SQL connection used for database operations
     * @param hologramManager The HologramManager instance
     */
    public PenalizeCommand(JavaPlugin plugin, Connection connection, HologramManager hologramManager) {
        this.plugin = plugin;
        this.connection = connection;
        this.hologramManager = hologramManager;
    }

    /**
     * Executes the `/penalize` command.
     * Only accessible by server operators (OPs). Applies a penalty to a specified player,
     * which includes logging the penalty and crediting it to a piggy bank table.
     *
     * @param sender  The command sender
     * @param command The command executed
     * @param label   The command label used
     * @param args    The command arguments
     * @return {@code true} if the command was processed
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Only OP's are allowed to run this command!", NamedTextColor.RED));
            return true;
        }

        // Return false if less than 3 arguments are given
        if (args.length < 3) return false;

        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null) return false;

        double penaltyAmount;
        try {
            penaltyAmount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number!", NamedTextColor.RED));
            return false;
        }

        String penaltyReason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        try {
            addPenalty(targetPlayer, penaltyAmount, penaltyReason);
            addToPiggyBank(targetPlayer, penaltyAmount);
            hologramManager.updateHologram(HologramEnum.PIGGY_BANK);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed creating penalty!");
            e.printStackTrace();

            sender.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
            return false;
        }

        // Construct penalty message
        String penaltyMessage = plugin.getConfig().getString("messages.penalty-given", "%player% got a penalty of %amount% for: %reason%!")
                .replace("%player%", targetPlayer.getName())
                .replace("%amount%", String.valueOf(penaltyAmount))
                .replace("%reason%", penaltyReason);

        Component messageComponent = Component.text(penaltyMessage)
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD);

        // Play sound for all players and send serverwide penalty message
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player, Sound.AMBIENT_CAVE, 2, 1);
        }
        plugin.getServer().broadcast(messageComponent);

        // Get Discord channel
        String discordChannelId = DiscordSRV.config().getString("Channels.announcements");
        TextChannel discordChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordChannelId);

        if (discordChannel == null) {
            plugin.getLogger().warning("DiscordSRV: channel not found!");
            return true;
        }

        // Set up Discord embed message
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Penalty Alert!");
        embed.setDescription(penaltyMessage);
        embed.setThumbnail(PlayerAvatarUtil.getPlayerAvatarUrl(targetPlayer, 50));
        embed.setColor(Color.RED);

        // Send Discord messages
        discordChannel.sendMessage("||@everyone||").queue();
        discordChannel.sendMessageEmbeds(embed.build()).queue();

        return true;
    }

    /**
     * Inserts a penalty record for a player into the `penalties` database table.
     *
     * @param targetPlayer  The player receiving the penalty
     * @param penaltyAmount The amount of the penalty
     * @param reason        The reason for the penalty
     * @throws SQLException If a database error occurs
     */
    private void addPenalty(Player targetPlayer, double penaltyAmount, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO penalties (player_uuid, reason, amount)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, targetPlayer.getUniqueId().toString());
            statement.setString(2, reason);
            statement.setDouble(3, penaltyAmount);
            statement.execute();
        }
    }

    /**
     * Credits the penalty amount to the player's piggy bank as a penalty entry.
     *
     * @param targetPlayer  The player whose piggy bank is credited
     * @param penaltyAmount The amount added to the piggy bank
     * @throws SQLException If a database error occurs
     */
    private void addToPiggyBank(Player targetPlayer, double penaltyAmount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO piggy_bank (player_uuid, amount, is_penalty)
                VALUES (?, ?, 1)
                """)) {
            statement.setString(1, targetPlayer.getUniqueId().toString());
            statement.setDouble(2, penaltyAmount);
            statement.execute();
        }
    }
}
