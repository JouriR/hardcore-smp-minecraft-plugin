package com.jouriroosjen.hardcoreSMPPlugin.managers;

import com.jouriroosjen.hardcoreSMPPlugin.enums.HologramEnum;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Manages the holograms for this plugin.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class HologramManager extends eu.decentsoftware.holograms.api.holograms.HologramManager {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Constructs a new {@code HologramManager} instance.
     *
     * @param decentHolograms The initialised {@code DecentHolograms} instance.
     * @param plugin          The main plugin instance.
     * @param connection      The active database connection.
     */
    public HologramManager(DecentHolograms decentHolograms, JavaPlugin plugin, Connection connection) {
        super(decentHolograms);

        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Creates a new hologram and registers it to this manager.
     *
     * @param hologramName     The name of the hologram.
     * @param hologramLocation The spawn location for the hologram.
     * @param type             The hologram type.
     * @param player           The player trying to create the hologram.
     */
    public void createHologram(String hologramName, Location hologramLocation, HologramEnum type, Player player) {
        switch (type) {
            case HologramEnum.PIGGY_BANK -> {
                Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, true);
                makePiggyBankHologram(hologram, player);
            }
            default ->
                    player.sendMessage(Component.text("Unknown hologram type!", NamedTextColor.RED, TextDecoration.BOLD));
        }
    }

    /**
     * Fill given hologram for piggy bank showcase
     *
     * @param hologram The hologram instance to fill.
     * @param player   The player trying to create the hologram.
     */
    private void makePiggyBankHologram(Hologram hologram, Player player) {
        double piggyBankAmount = 0;
        try {
            piggyBankAmount = getPiggyBankTotal();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get piggy bank total!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
        }

        String piggyBankName = plugin.getConfig().getString("piggy-bank-name", "Piggy bank");

        DHAPI.addHologramLine(hologram, "&u&l&n" + piggyBankName);
        DHAPI.addHologramLine(hologram, "€" + piggyBankAmount);
        DHAPI.addHologramLine(hologram, "#ENTITY: PIG");

        this.registerHologram(hologram);
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
