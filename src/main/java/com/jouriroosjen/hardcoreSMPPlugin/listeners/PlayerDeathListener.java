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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Listener class for handling player death events in the server.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerDeathListener implements Listener {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final HologramManager hologramManager;

    private static final int MAX_HUNGER = 20;
    private static final int PARTICLE_COUNT = 100;
    private static final float PARTICLE_SIZE = 1.5f;
    private static final double PARTICLE_OFFSET_RANGE = 2.0;

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
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Cancel the default death sequence
        event.setCancelled(true);

        final Player player = event.getEntity();
        if (player == null) return;

        final Location deathLocation = player.getLocation();
        final String deathCause = extractDeathCause(player);
        final String playerName = player.getName();

        // Execute custom death sequence
        executeDeathEffects(player, deathLocation);
        handleInventoryDrop(player, deathLocation);
        handleExperienceDrop(player, deathLocation);
        updatePlayerState(player);
        broadcastDeathMessage(playerName);

        // Handle database operations asynchronously to avoid blocking main thread
        CompletableFuture<Void> databaseOperations = CompletableFuture.runAsync(() -> {
            try {
                saveDeathAndUpdateStatus(player.getUniqueId(), deathCause);
            } catch (SQLException e) {
                plugin.getLogger().severe("[DATABASE] Failed to save death and update status for player " + playerName);
                e.printStackTrace();
            }
        });

        // Update hologram after database operations complete
        databaseOperations.thenRun(() -> {
            // Run hologram update back on main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        hologramManager.updateHologram(HologramEnum.LATEST_DEATH);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to update hologram after death: " + e.getMessage());
                    }
                }
            }.runTask(plugin);
        });

        // Send death message in Discord
        handleDiscordNotification(player);
    }

    /**
     * Executes death effects.
     *
     * @param player        The player who died.
     * @param deathLocation The location where the player died.
     */
    private void executeDeathEffects(Player player, Location deathLocation) {
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);

        World world = deathLocation.getWorld();
        if (world != null)
            world.playSound(deathLocation, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);

        spawnDeathParticles(deathLocation);
    }

    /**
     * Spawns dust particles at the death location.
     *
     * @param deathLocation The location to spawn the particles.
     */
    private void spawnDeathParticles(Location deathLocation) {
        World world = deathLocation.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, PARTICLE_SIZE);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double offsetX = (Math.random() - 0.5) * PARTICLE_OFFSET_RANGE;
            double offsetY = (Math.random() - 0.5) * PARTICLE_OFFSET_RANGE;
            double offsetZ = (Math.random() - 0.5) * PARTICLE_OFFSET_RANGE;

            Location particleLocation = deathLocation.clone().add(offsetX, offsetY, offsetZ);
            world.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, dustOptions);
        }
    }

    /**
     * Handles dropping the player's inventory.
     *
     * @param player        The player who died.
     * @param deathLocation The location to drop the items.
     */
    private void handleInventoryDrop(Player player, Location deathLocation) {
        World world = deathLocation.getWorld();
        if (world == null) return;

        ItemStack[] inventory = player.getInventory().getContents();
        for (ItemStack item : inventory) {
            if (item != null && !item.getType().isAir())
                world.dropItemNaturally(deathLocation, item);
        }

        // Clear inventory to prevent ghost items
        player.getInventory().clear();
    }

    private void handleExperienceDrop(Player player, Location deathLocation) {
        int deathXp = calculateDeathXp(player);

        if (deathXp > 0) {
            World world = deathLocation.getWorld();
            if (world != null)
                world.spawn(deathLocation, ExperienceOrb.class, orb -> orb.setExperience(deathXp));
        }

        // Reset player experience
        player.setTotalExperience(0);
        player.setExp(0);
        player.setLevel(0);
    }

    /**
     * Calculates the amount of xp that should be dropped when a player dies.
     * Vanilla drops between 0 and 7 * level, capped at 100xp.
     *
     * @param player The player that died
     * @return The amount of experience points to drop
     */
    private int calculateDeathXp(Player player) {
        return Math.min(player.getLevel() * 7, 100);
    }

    /**
     * Updates the player's state after death.
     *
     * @param player The player who died.
     */
    private void updatePlayerState(Player player) {
        // Restore hunger
        player.setFoodLevel(MAX_HUNGER);

        // Set to spectator mode and play ambient sound
        player.setGameMode(GameMode.SPECTATOR);
        player.playSound(player, Sound.AMBIENT_CAVE, 1.0f, 1.0f);
    }

    /**
     * Broadcasts a death message to all players on the server.
     *
     * @param playerName The name of the player who died.
     */
    private void broadcastDeathMessage(String playerName) {
        String deathMessage = plugin.getConfig().getString("messages.death", "has died!");

        TextComponent messageComponent = Component.text()
                .content("[SERVER] ")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" "))
                .append(Component.text(deathMessage, NamedTextColor.RED))
                .build();

        plugin.getServer().broadcast(messageComponent);
    }

    /**
     * Handles Discord death message asynchronously.
     *
     * @param player The player who died.
     */
    private void handleDiscordNotification(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                sendDiscordDeathNotification(player);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send Discord death notification for player " + player.getName());
                e.printStackTrace();
            }
        });
    }

    /**
     * Sends a death notification to Discord with processed avatar image.
     *
     * @param player The player who died.
     */
    private void sendDiscordDeathNotification(Player player) {
        String discordChannelId = DiscordSRV.config().getString("Channels.deaths");
        if (discordChannelId == null || discordChannelId.isEmpty()) {
            plugin.getLogger().warning("DiscordSRV: No deaths channel configured!");
            return;
        }

        TextChannel discordChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordChannelId);
        if (discordChannel == null) {
            plugin.getLogger().warning("DiscordSRV: Deaths channel not found with ID: " + discordChannelId);
            return;
        }

        String playerName = player.getName();

        try {
            // Get and process avatar image
            String deathMessage = plugin.getConfig().getString("messages.death", "has died!");
            BufferedImage processedAvatar = createDeathAvatar(player, deathMessage);

            // Convert image to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedAvatar, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // Send Discord message
            discordChannel.sendMessage("||@everyone||")
                    .addFile(new ByteArrayInputStream(imageBytes), "death_avatar.png")
                    .queue(
                            success -> plugin.getLogger().info("Death notification sent to Discord for " + playerName),
                            failure -> plugin.getLogger().warning("Failed to send Discord message: " + failure.getMessage())
                    );
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create or send death avatar for " + playerName);
            e.printStackTrace();
        }
    }

    /**
     * Creates a processed death avatar image.
     *
     * @param player       The player who died.
     * @param deathMessage The death message.
     * @return Processed death avatar image.
     * @throws Exception If image processing fails.
     */
    private BufferedImage createDeathAvatar(Player player, String deathMessage) throws Exception {
        // Get original avatar
        String avatarUrl = PlayerAvatarUtil.getPlayerAvatarUrl(player, 300);
        BufferedImage originalAvatar = ImageIO.read(new URL(avatarUrl));

        // Apply death effects
        BufferedImage processedAvatar = ImageUtils.toGrayscale(originalAvatar);
        ImageUtils.drawRedCross(processedAvatar);

        // Add death text if font resource is available
        try (var fontStream = plugin.getResource("fonts/MinecraftBold.otf")) {
            if (fontStream != null)
                ImageUtils.drawDeathText(processedAvatar, player.getName(), deathMessage, fontStream);
        }

        return processedAvatar;
    }

    /**
     * Extracts the death cause from the player's death.
     *
     * @param player The player who died.
     * @return The death cause.
     */
    private String extractDeathCause(Player player) {
        try {
            var lastDamageCause = player.getLastDamageCause();
            return lastDamageCause != null ? lastDamageCause.getCause().toString() : "UNKNOWN";
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract death cause for player " + player.getName() + ": " + e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Saves death to database and updates player alive status.
     *
     * @param playerUuid The UUID of the player who died.
     * @param cause      The cause of death.
     * @throws SQLException If a database error occurs.
     */
    private void saveDeathAndUpdateStatus(UUID playerUuid, String cause) throws SQLException {
        connection.setAutoCommit(false);

        try (PreparedStatement deathStatement = connection.prepareStatement("""
                INSERT INTO deaths (player_uuid, cause) 
                VALUES (?, ?)
                """);
             PreparedStatement statusStatement = connection.prepareStatement("""
                     UPDATE players SET
                         is_alive = 0,
                         updated_at = datetime('now') 
                     WHERE uuid = ?
                     """)) {
            // Insert death record
            deathStatement.setString(1, playerUuid.toString());
            deathStatement.setString(2, cause);
            deathStatement.execute();

            // Update alive status
            statusStatement.setString(1, playerUuid.toString());
            statusStatement.executeUpdate();

            // Commit transaction
            connection.commit();

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                plugin.getLogger().severe("Failed to rollback transaction: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }
}
