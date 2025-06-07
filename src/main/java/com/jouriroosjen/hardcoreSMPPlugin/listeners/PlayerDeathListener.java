package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * Listener class for handling player death events in the server.
 *
 * @author Jouri Roosjen
 * @version 0.1.0
 */
public class PlayerDeathListener implements Listener {
    private final JavaPlugin plugin;
    private final Connection connection;

    /**
     * Constructs a new {@code PlayerDeathListener} instance.
     *
     * @param plugin The main plugin instance
     * @param connection The active database connection
     */
    public PlayerDeathListener(JavaPlugin plugin,  Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Handles the {@link PlayerDeathEvent}. Overrides the default death behavior
     * by canceling the event and applying a custom death sequence:
     * <ul>
     *     <li>Plays sounds for dramatic effect</li>
     *     <li>Spawns red dust particles in random directions</li>
     *     <li>Drops the player's inventory</li>
     *     <li>Drops an XP orb</li>
     *     <li>Switches the player to spectator mode</li>
     *     <li>Broadcasts a death message to all players</li>
     *     <li>Saves death in database {@code deaths} table</li>
     * </ul>
     *
     * @param event The player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Cancel the default death behavior
        event.setCancelled(true);

        final Player player = event.getEntity();
        final Location loc = player.getLocation();

        // Play death sound effects
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1, 1);
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);

        // Spawn particle effects at the player's death location
        for (int i = 0; i < 100; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = (Math.random() - 0.5) * 2;
            double offsetZ = (Math.random() - 0.5) * 2;

            loc.getWorld().spawnParticle(
                    Particle.DUST,
                    loc.clone().add(offsetX, offsetY, offsetZ),
                    1,
                    0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 1.5f)
            );
        }

        // Drop inventory and clear manually to prevent ghost items
        ItemStack[] playerInventory = player.getInventory().getContents();
        for (ItemStack drop : playerInventory) {
            if (drop != null && !drop.getType().isAir()) {
                loc.getWorld().dropItemNaturally(loc, drop);
            }
        }
        player.getInventory().clear();

        // Spawn xp orb
        int deathXp = getDeathXp(player);
        if (deathXp > 0) {
            loc.getWorld().spawn(loc, ExperienceOrb.class, orb -> {
                orb.setExperience(deathXp);
            });
        }
        player.setTotalExperience(0);
        player.setExp(0);
        player.setLevel(0);

        // Set player to spectator mode and play ambient sound
        player.setGameMode(GameMode.SPECTATOR);
        player.playSound(player, Sound.AMBIENT_CAVE, 1, 1);

        // Broadcast custom death message to the server
        String deathMessage = plugin.getConfig().getString("general.death-message");
        TextComponent messageComponent = Component.text()
                .content("[SERVER] ")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" "))
                .append(Component.text(Objects.requireNonNull(deathMessage), NamedTextColor.RED))
                .build();

        plugin.getServer().broadcast(messageComponent);

        // Save death to database
        try {
            saveDeathToDatabase(player.getUniqueId());
        } catch (SQLException e) {
            plugin.getLogger().severe("[DATABASE] Failed to save death of player " + player.getName() + " - " + player.getUniqueId());
            e.printStackTrace();
        }
    }

    /**
     * Calculates the amount of xp that should be dropped when a player dies.
     * Vanilla drops between 0 and 7 * level, capped at 100xp.
     *
     * @param player The player that died
     * @return The amount of experience points to drop
     */
    private int getDeathXp(Player player) {
        int level = player.getLevel();
        return (int) Math.min(player.getLevel() * 7, 100);
    }

    /**
     * Inserts a player's death into the {@code deaths} table.
     *
     * @param playerUuid The UUID of the player who died
     * @throws SQLException If a database error occurs while inserting
     */
    private void saveDeathToDatabase(UUID playerUuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO deaths (uuid) VALUES (?)")) {
            preparedStatement.setString(1, String.valueOf(playerUuid));
            preparedStatement.execute();
        }
    }
}
