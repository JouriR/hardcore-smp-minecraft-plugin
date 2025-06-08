package com.jouriroosjen.hardcoreSMPPlugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Manages buyback confirmations initiated by players.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class BuybackManager {
    private final Map<UUID, PendingBuyback> pendingConformations = new HashMap<>();

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
        pendingConformations.put(sender, new PendingBuyback(target, percentage));
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
}
