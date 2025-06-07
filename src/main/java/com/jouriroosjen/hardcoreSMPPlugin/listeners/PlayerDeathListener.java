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

import java.util.List;

/**
 * Listener class for handling player death events in the server.
 *
 * @author Jouri Roosjen
 * @version 0.1.0
 */
public class PlayerDeathListener implements Listener {
    private final JavaPlugin plugin;

    /**
     * Constructs a new {@code PlayerDeathListener}.
     *
     * @param plugin The main plugin instance
     */
    public PlayerDeathListener(JavaPlugin plugin) {
        this.plugin = plugin;
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
        final TextComponent message = Component.text()
                .content("[SERVER] ")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" moet dokken!", NamedTextColor.RED))
                .build();

        plugin.getServer().broadcast(message);
    }

    /**
     * Calculates the amount of xp that should be dropped when a player dies.
     * Vanilla drops between 0 and 7 * level, capped at 100xp.
     *
     * @param player The player that died
     * @return int
     */
    private int getDeathXp(Player player) {
        int level = player.getLevel();
        return (int) Math.min(player.getLevel() * 7, 100);
    }
}
