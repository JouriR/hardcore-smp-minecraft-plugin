package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Set;

/**
 * Handles player advancement done events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerAdvancementDoneListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Set<String> TRACKED_ADVANCEMENT_KEYS = Set.of(
            "story", "nether", "end",
            "adventure", "husbandry"
    );

    /**
     * Constructs a new {@code PlayerAdvancementDoneListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerAdvancementDoneListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player advancement done events.
     *
     * @param event The player advancement done event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        if (advancement == null) return;

        NamespacedKey key = advancement.getKey();
        if (key == null) return;

        String keyString = key.getKey().split("/")[0];
        if (!TRACKED_ADVANCEMENT_KEYS.contains(keyString)) return;

        playerStatisticsManager.incrementStatistic(event.getPlayer().getUniqueId(), PlayerStatisticsEnum.ADVANCEMENTS_DONE, 1);
    }
}
