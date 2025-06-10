package com.jouriroosjen.hardcoreSMPPlugin.commands;

import net.kyori.adventure.text.Component;
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
import java.util.UUID;

/**
 * Command executor for the {@code /my-debt} command, allowing players to see their debt.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class MyDebtCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Constructs a new {@code MyDebtCommand} instance.
     *
     * @param plugin     The main plugin instance
     * @param connection The active database connection
     */
    public MyDebtCommand(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Executes the my-debt command logic when a player runs it.
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

        try {
            UUID playerUuid = player.getUniqueId();
            double totalDebt = getTotalDebt(playerUuid);

            String debtMessage = plugin.getConfig().getString("messages.debt", "You have a debt of €%amount%!")
                    .replace("%amount%", String.valueOf(totalDebt));
            Component messageComponent = Component.text(debtMessage, NamedTextColor.BLUE);

            player.sendMessage(messageComponent);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed getting total debt for player: " + player.getName());
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
            return false;
        }
    }

    /**
     * Get the total debt for the given player
     *
     * @param playerUuid The player's UUID
     * @return The total debt of a player
     * @throws SQLException If a database error occurs
     */
    private double getTotalDebt(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT SUM(amount) AS total_debt
                FROM piggy_bank
                WHERE player_uuid = ?
                """)) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("total_debt");
                }
            }
        }

        return 0;
    }
}
