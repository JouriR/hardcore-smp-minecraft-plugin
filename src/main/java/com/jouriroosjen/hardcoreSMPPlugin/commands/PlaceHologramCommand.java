package com.jouriroosjen.hardcoreSMPPlugin.commands;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
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

/**
 * Command executor for the {@code /place-hologram} command.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class PlaceHologramCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Constructs a new {@code PlaceHologramCommand} instance.
     *
     * @param plugin     The main plugin instance.
     * @param connection The active database connection.
     */
    public PlaceHologramCommand(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Executes the place-hologram command logic when a player runs it.
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

        if (!player.isOp()) {
            sender.sendMessage(Component.text("Only OP's are allowed to run this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) return false;

        String hologramName = args[0];
        Hologram hologram = DHAPI.getHologram(hologramName);

        if (hologram == null) {
            Location hologramLocation = player.getLocation().add(0, 1.75, 0);
            hologram = DHAPI.createHologram(hologramName, hologramLocation, true);
        }

        double piggyBankAmount;
        try {
            piggyBankAmount = getPiggyBankTotal();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get piggy bank total!");
            e.printStackTrace();

            sender.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
            return false;
        }

        String piggyBankName = plugin.getConfig().getString("piggy-bank-name", "Piggy bank");

        DHAPI.addHologramLine(hologram, "&u&l&n" + piggyBankName);
        DHAPI.addHologramLine(hologram, "€" + piggyBankAmount);
        DHAPI.addHologramLine(hologram, "#ENTITY: PIG");

        return true;
    }

    /**
     * Gets the total amount currently in the piggy bank.
     *
     * @return The total amount stored in the piggy bank.
     * @throws SQLException If a database error occurs.
     */
    private double getPiggyBankTotal() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT SUM(amount) AS total
                FROM piggy_bank
                """)) {
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble("total");
            }
        }
        return 0;
    }
}
