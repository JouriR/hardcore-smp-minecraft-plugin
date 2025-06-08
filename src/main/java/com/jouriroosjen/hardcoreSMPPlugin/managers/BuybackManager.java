package com.jouriroosjen.hardcoreSMPPlugin.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Manages buyback confirmations initiated by players.
 *
 * @author Jouri Roosjen
 * @version 1.1.0
 */
public class BuybackManager {
    private final JavaPlugin plugin;

    private final Map<UUID, PendingBuyback> pendingConformations = new HashMap<>();

    /**
     * Constructs a new {@code BuybackManager} instance.
     *
     * @param plugin The main plugin instance
     */
    public BuybackManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Represents a pending buyback.
     *
     * @param target     The UUID of the player to be revived
     * @param percentage An optional percentage value in case of an assist
     */
    public record PendingBuyback(UUID target, OptionalInt percentage) {
    }

    /**
     * Adds a new pending buyback confirmation for the given sender.
     *
     * @param sender     The UUID of the player initiating the buyback
     * @param target     The UUID of the player to be revived
     * @param percentage An optional percentage value in case of an assist
     */
    public void addPending(UUID sender, UUID target, OptionalInt percentage) {
        PendingBuyback buyback = new PendingBuyback(target, percentage);
        pendingConformations.put(sender, buyback);

        // Schedule auto remove after 60 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                PendingBuyback existing = pendingConformations.get(sender);
                if (existing != null && existing.equals(buyback)) {
                    pendingConformations.remove(sender);
                    Player player = Bukkit.getPlayer(sender);

                    if (player != null) {
                        player.sendMessage(Component.text("Your buyback/assist request has expired!", NamedTextColor.RED));
                    }
                }
            }
        }.runTaskLater(plugin, 20 * 60);
    }

    /**
     * Checks if the specified sender has a pending buyback confirmation.
     *
     * @param sender The UUID of the player to check
     * @return {@code true} if the sender has a pending confirmation, {@code false} otherwise
     */
    public boolean hasPending(UUID sender) {
        return pendingConformations.containsKey(sender);
    }

    /**
     * Confirms and removes the pending buyback for the specified sender.
     *
     * @param sender The UUID of the player confirming the buyback
     * @return The associated PendingBuyback data, or null if none exists
     */
    public PendingBuyback confirm(UUID sender) {
        return pendingConformations.remove(sender);
    }

    /**
     * Clear all pending confirmations
     */
    public void clear() {
        pendingConformations.clear();
    }
}
