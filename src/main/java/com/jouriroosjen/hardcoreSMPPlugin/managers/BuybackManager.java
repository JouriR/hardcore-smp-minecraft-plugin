package com.jouriroosjen.hardcoreSMPPlugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

public class BuybackManager {
    private final Map<UUID, PendingBuyback> pendingConformations = new HashMap<>();

    public record PendingBuyback(UUID target, OptionalInt percentage) {
    }

    public void addPending(UUID sender, UUID target, OptionalInt percentage) {
        pendingConformations.put(sender, new PendingBuyback(target, percentage));
    }

    public boolean hasPending(UUID sender) {
        return pendingConformations.containsKey(sender);
    }

    public PendingBuyback confirm(UUID sender) {
        return pendingConformations.remove(sender);
    }
}
