package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.HologramEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.HologramManager;
import com.jouriroosjen.hardcoreSMPPlugin.utils.ImageUtils;
import com.jouriroosjen.hardcoreSMPPlugin.utils.PlayerAvatarUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Listener class for handling player death events in the server.
 *
 * @author Jouri Roosjen
 * @version 0.2.1
 */
public class PlayerDeathListener implements Listener {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final HologramManager hologramManager;

    /**
     * Constructs a new {@code PlayerDeathListener} instance.
     *
     * @param plugin          The main plugin instance
     * @param connection      The active database connection
     * @param hologramManager The hologram manager instance
     */
    public PlayerDeathListener(JavaPlugin plugin, Connection connection, HologramManager hologramManager) {
        this.plugin = plugin;
        this.connection = connection;
        this.hologramManager = hologramManager;
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

        // Fill player's hunger after "death"
        player.setFoodLevel(20);

        // Set player to spectator mode and play ambient sound
        player.setGameMode(GameMode.SPECTATOR);
        player.playSound(player, Sound.AMBIENT_CAVE, 1, 1);

        // Broadcast custom death message to the server
        String deathMessage = plugin.getConfig().getString("messages.death", "has died!");
        TextComponent messageComponent = Component.text()
                .content("[SERVER] ")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" "))
                .append(Component.text(deathMessage, NamedTextColor.RED))
                .build();

        plugin.getServer().broadcast(messageComponent);

        // Save death to database
        try {
            saveDeathToDatabase(player.getUniqueId());
        } catch (SQLException e) {
            plugin.getLogger().severe("[DATABASE] Failed to save death of player " + player.getName());
            e.printStackTrace();
        }

        // Update player alive status
        try {
            updatePlayerAliveStatus(player.getUniqueId());
        } catch (SQLException e) {
            plugin.getLogger().severe("[DATABASE] Failed to update alive status of player " + player.getName());
            e.printStackTrace();
        }

        hologramManager.updateHologram(HologramEnum.LATEST_DEATH);

        String discordChannelId = DiscordSRV.config().getString("Channels.global");
        TextChannel discordChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordChannelId);

        if (discordChannel == null) {
            plugin.getLogger().warning("DiscordSRV: channel not found!");
            return;
        }

        try {
            // Get avatar image and apply filters
            BufferedImage originalAvatar = ImageIO.read(new URL(PlayerAvatarUtil.getPlayerAvatarUrl(player, 500)));
            BufferedImage processedAvatar = ImageUtils.toGrayscale(originalAvatar);
            ImageUtils.drawRedCross(processedAvatar);
            ImageUtils.drawDeathText(processedAvatar, player.getName(), deathMessage, plugin.getResource("fonts/MinecraftBold.otf"));

            // Convert the image to a byte array in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedAvatar, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // Send message with attached avatar image
            discordChannel.sendMessage("||@everyone||")
                    .addFile(new ByteArrayInputStream(imageBytes), "avatar.png")
                    .queue();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send death embed of player " + player.getName());
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
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO deaths (player_uuid) VALUES (?)")) {
            statement.setString(1, playerUuid.toString());
            statement.execute();
        }
    }

    /**
     * Updates the player's alive status in the database to indicate they are no longer alive.
     *
     * @param playerUuid The UUID of the player to update
     * @throws SQLException If a database access error occurs
     */
    private void updatePlayerAliveStatus(UUID playerUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                   UPDATE players SET
                       is_alive = 0,
                       updated_at = datetime('now')
                   WHERE uuid = ?
                """)) {
            statement.setString(1, playerUuid.toString());
            statement.execute();
        }
    }
}
