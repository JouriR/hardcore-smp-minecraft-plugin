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
import java.util.UUID;

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
            case PIGGY_BANK -> {
                Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, true);
                constructPiggyBankHologram(hologram, player);
            }
            case LATEST_DEATH -> {
                Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, true);
                constructLatestDeathHologram(hologram, player);
            }
            default ->
                    player.sendMessage(Component.text("Unknown hologram type!", NamedTextColor.RED, TextDecoration.BOLD));
        }
    }

    /**
     * Update the specified hologram with new data.
     *
     * @param hologramName The name of the hologram to update.
     */
    public void updateHologram(HologramEnum hologramName) {
        Hologram hologram = this.getHologram(hologramName.toString());
        if (hologram == null) {
            plugin.getLogger().severe("Failed to get hologram: " + hologramName);
            return;
        }

        switch (hologramName) {
            case PIGGY_BANK -> {
                try {
                    double piggyBankAmmount = getPiggyBankTotal();
                    DHAPI.setHologramLine(hologram, 1, "€" + piggyBankAmmount);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to update piggy bank hologram!");
                    e.printStackTrace();
                }
            }
            case LATEST_DEATH -> {
                try {
                    UUID deathPlayerUuid = getLatestDeathUuid();
                    Player deathPlayer = plugin.getServer().getPlayer(deathPlayerUuid);
                    String deathHologramText = plugin.getConfig().getString("holograms.latest-death", "Latest death:");

                    DHAPI.setHologramLine(hologram, 0, "&4&l&n" + deathHologramText);
                    DHAPI.setHologramLine(hologram, 1, deathPlayer.getName());
                    DHAPI.setHologramLine(hologram, 2, "#HEAD: PLAYER_HEAD (" + deathPlayer.getName() + ")");
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to update latest death hologram!");
                    e.printStackTrace();
                }
            }
            default -> plugin.getLogger().severe("Unknown hologram type!");
        }
    }

    /**
     * Fill given hologram for piggy bank showcase
     *
     * @param hologram The hologram instance to fill.
     * @param player   The player trying to create the hologram.
     */
    private void constructPiggyBankHologram(Hologram hologram, Player player) {
        double piggyBankAmount = 0;
        try {
            piggyBankAmount = getPiggyBankTotal();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get piggy bank total!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
            return;
        }

        String piggyBankName = plugin.getConfig().getString("holograms.piggy-bank-name", "Piggy bank");

        DHAPI.addHologramLine(hologram, "&u&l&n" + piggyBankName);
        DHAPI.addHologramLine(hologram, "€" + piggyBankAmount);
        DHAPI.addHologramLine(hologram, "#ENTITY: PIG");

        this.registerHologram(hologram);
    }

    /**
     * Fill given hologram for latest death hologram
     *
     * @param hologram The hologram to be filled
     * @param player   The player trying to create the hologram
     */
    private void constructLatestDeathHologram(Hologram hologram, Player player) {
        UUID latestDeathPlayerUuid;
        try {
            latestDeathPlayerUuid = getLatestDeathUuid();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get latest death UUID!");
            e.printStackTrace();

            player.sendMessage(Component.text("Internal database error.", NamedTextColor.RED, TextDecoration.BOLD));
            return;
        }

        if (latestDeathPlayerUuid != null) {
            String latestDeathHologramText = plugin.getConfig().getString("holograms.latest-death", "Latest death:");
            Player latestDeathPlayer = plugin.getServer().getPlayer(latestDeathPlayerUuid);

            DHAPI.addHologramLine(hologram, "&4&l&n" + latestDeathHologramText);
            DHAPI.addHologramLine(hologram, latestDeathPlayer.getName());
            DHAPI.addHologramLine(hologram, "#HEAD: PLAYER_HEAD (" + latestDeathPlayer.getName() + ")");
        } else {
            String noLatestDeathText = plugin.getConfig().getString("holograms.no-latest-death", "No deaths yet!");

            DHAPI.addHologramLine(hologram, "&4&l&n" + noLatestDeathText);
            DHAPI.addHologramLine(hologram, "");
            DHAPI.addHologramLine(hologram, "");
        }

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

    /**
     * Get the player UUID of the latest death
     *
     * @return A player UUID
     * @throws SQLException If a database error occurs
     */
    private UUID getLatestDeathUuid() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (player_uuid) FROM deaths
                ORDER BY datetime(created_at) DESC
                LIMIT 1
                """)) {
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return UUID.fromString(resultSet.getString("player_uuid"));
            }
        }
        return null;
    }
}
