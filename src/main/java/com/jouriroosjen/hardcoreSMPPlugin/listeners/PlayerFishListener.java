package com.jouriroosjen.hardcoreSMPPlugin.listeners;

import com.jouriroosjen.hardcoreSMPPlugin.enums.PlayerStatisticsEnum;
import com.jouriroosjen.hardcoreSMPPlugin.managers.PlayerStatisticsManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player fish events.
 *
 * @author Jouri Roosjen
 * @version 2.0.0
 */
public class PlayerFishListener implements Listener {
    private final PlayerStatisticsManager playerStatisticsManager;

    private static final Map<Material, PlayerStatisticsEnum> FISH_MATERIAL_TO_STAT = new EnumMap<>(Material.class);

    static {
        FISH_MATERIAL_TO_STAT.put(Material.LILY_PAD, PlayerStatisticsEnum.LILLY_PAD_CAUGHT);
        FISH_MATERIAL_TO_STAT.put(Material.PUFFERFISH, PlayerStatisticsEnum.PUFFERFISH_CAUGHT);
        FISH_MATERIAL_TO_STAT.put(Material.COD, PlayerStatisticsEnum.FISH_CAUGHT);
        FISH_MATERIAL_TO_STAT.put(Material.SALMON, PlayerStatisticsEnum.FISH_CAUGHT);
        FISH_MATERIAL_TO_STAT.put(Material.TROPICAL_FISH, PlayerStatisticsEnum.FISH_CAUGHT);
    }

    /**
     * Construct a new {@code PlayerFishListener} instance.
     *
     * @param playerStatisticsManager The {@code playerStatisticsManager} instance.
     */
    public PlayerFishListener(PlayerStatisticsManager playerStatisticsManager) {
        this.playerStatisticsManager = playerStatisticsManager;
    }

    /**
     * Event handler for player fish event.
     *
     * @param event The player fish event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        PlayerFishEvent.State state = event.getState();

        switch (state) {
            case CAUGHT_FISH -> handleCaughtFish(playerUuid, event);
            case CAUGHT_ENTITY -> handleCaughtEntity(playerUuid, event);
            case IN_GROUND ->
                    playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.FISH_HOOK_IN_GROUND, 1);
            case FAILED_ATTEMPT ->
                    playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.FISHING_FAILED, 1);
            default -> {
            }
        }
    }

    /**
     * Handles a caught fish.
     *
     * @param playerUuid The UUID of the player.
     * @param event      The player fish event.
     */
    private void handleCaughtFish(UUID playerUuid, PlayerFishEvent event) {
        Entity caughtEntity = event.getCaught();
        if (!(caughtEntity instanceof Item caughtItem)) return;

        ItemStack itemStack = caughtItem.getItemStack();
        Material material = itemStack.getType();

        PlayerStatisticsEnum statistic = FISH_MATERIAL_TO_STAT.get(material);
        if (statistic == null) return;

        playerStatisticsManager.incrementStatistic(playerUuid, statistic, 1);
    }

    /**
     * Handles entity catching.
     *
     * @param playerUuid The UUID of the player.
     * @param event      The fishing event.
     */
    private void handleCaughtEntity(UUID playerUuid, PlayerFishEvent event) {
        Entity hookedEntity = event.getHook().getHookedEntity();
        if (!(hookedEntity instanceof Player)) return;

        playerStatisticsManager.incrementStatistic(playerUuid, PlayerStatisticsEnum.FISH_HOOK_IN_PLAYER, 1);
    }
}
